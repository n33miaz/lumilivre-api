package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_LOAN_REQUESTS;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_LIST;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_STATS;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy;
import br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.LoanRequest;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.LoanRequestRepository;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.security.Auditable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanRequestService {

    private final StudentRepository studentRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final OutboxPublisherService outboxPublisher;

    public List<LoanRequest> listAll() {
        return loanRequestRepository.findAllByOrderByRequestedAtDesc();
    }

    public List<LoanRequest> listPending() {
        return loanRequestRepository.findByStatus(LoanRequestStatus.PENDING);
    }

    public List<LoanRequest> listByStudent(String registrationNumber) {
        return loanRequestRepository.findByStudent_RegistrationNumberOrderByRequestedAtDesc(registrationNumber);
    }

    @Transactional
    @CacheEvict(value = DASHBOARD_LOAN_REQUESTS, allEntries = true)
    public ResponseEntity<String> solicitarEmprestimo(String matriculaAluno, String copyCode) {
        Student student = studentRepository.findByRegistrationNumber(matriculaAluno).orElse(null);
        if (student == null) return ResponseEntity.badRequest().body("Aluno não encontrado.");

        BookCopy bookCopy = bookCopyRepository.findByCopyCode(copyCode).orElse(null);
        if (bookCopy == null) return ResponseEntity.badRequest().body("Exemplar não encontrado.");

        long ativos = contarEmprestimosAtivos(matriculaAluno);
        RequestApprovalPolicy.validateRequest(student.getPenaltyExpiresAt(), ativos, bookCopy.getStatus());

        LoanRequest request = LoanRequest.builder()
                .student(student)
                .bookCopy(bookCopy)
                .build();
        loanRequestRepository.save(request);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, student.getEmail(), "Solicitação recebida",
                "Sua solicitação do livro '" + bookCopy.getBook().getTitle() + "' foi registrada.");

        return ResponseEntity.ok("Solicitação registrada com sucesso.");
    }

    @Transactional
    @CacheEvict(value = DASHBOARD_LOAN_REQUESTS, allEntries = true)
    public ResponseEntity<String> solicitarEmprestimoPorLivro(String matriculaAluno, UUID bookId) {
        Student student = studentRepository.findByRegistrationNumber(matriculaAluno).orElse(null);
        if (student == null) return ResponseEntity.badRequest().body("Aluno não encontrado.");

        BookCopy bookCopy = bookCopyRepository.findFirstAvailable(bookId).orElse(null);
        if (bookCopy == null)
            return ResponseEntity.badRequest().body("Não há exemplares disponíveis para este livro no momento.");

        long ativos = contarEmprestimosAtivos(matriculaAluno);
        RequestApprovalPolicy.validateRequest(student.getPenaltyExpiresAt(), ativos, bookCopy.getStatus());

        LoanRequest request = LoanRequest.builder()
                .student(student)
                .bookCopy(bookCopy)
                .note("Solicitado via Mobile")
                .build();
        loanRequestRepository.save(request);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, student.getEmail(), "Solicitação recebida",
                "Sua solicitação do livro '" + bookCopy.getBook().getTitle() + "' foi registrada.");

        return ResponseEntity.ok("Solicitação registrada com sucesso.");
    }

    @Auditable(action = "REQUEST_PROCESSED", targetParam = "#id")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = DASHBOARD_LOAN_REQUESTS, allEntries = true),
            @CacheEvict(value = DASHBOARD_STATS, allEntries = true),
            @CacheEvict(value = DASHBOARD_OVERDUE_LIST, allEntries = true)
    })
    public ResponseEntity<String> processarSolicitacao(UUID id, boolean aceitar) {
        LoanRequest request = loanRequestRepository.findById(id).orElse(null);
        if (request == null) return ResponseEntity.badRequest().body("Solicitação não encontrada.");

        Student student = request.getStudent();
        BookCopy bookCopy = request.getBookCopy();
        RequestApprovalPolicy.validateProcessable(request.getStatus());

        if (aceitar) {
            EmprestimoRequest dto = new EmprestimoRequest();
            dto.setAluno_matricula(student.getRegistrationNumber());
            dto.setExemplar_tombo(bookCopy.getCopyCode());
            dto.setData_emprestimo(OffsetDateTime.now());
            dto.setData_devolucao(OffsetDateTime.now().plusDays(14));
            loanService.cadastrar(dto);

            request.setStatus(LoanRequestStatus.ACCEPTED);
            loanRequestRepository.save(request);

            outboxPublisher.publish(EventType.REQUEST_ACCEPTED, student.getEmail(), "Solicitação aceita",
                    "Sua solicitação do livro '" + bookCopy.getBook().getTitle()
                            + "' foi aceita e o empréstimo registrado.");
        } else {
            request.setStatus(LoanRequestStatus.REJECTED);
            loanRequestRepository.save(request);

            outboxPublisher.publish(EventType.REQUEST_REJECTED, student.getEmail(), "Solicitação rejeitada",
                    "Sua solicitação do livro '" + bookCopy.getBook().getTitle() + "' foi rejeitada.");
        }

        return ResponseEntity.ok("Solicitação processada com sucesso.");
    }

    private long contarEmprestimosAtivos(String matricula) {
        return loanRepository.countByStudent_RegistrationNumberAndStatus(matricula, br.com.lumilivre.api.enums.LoanStatus.ACTIVE)
                + loanRepository.countByStudent_RegistrationNumberAndStatus(matricula, br.com.lumilivre.api.enums.LoanStatus.OVERDUE);
    }
}
