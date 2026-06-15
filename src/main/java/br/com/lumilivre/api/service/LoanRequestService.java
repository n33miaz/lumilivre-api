package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_LOAN_REQUESTS;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_LIST;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_STATS;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy;
import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
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

    private static final String REQUEST_CREATED_KEY = "request.created";
    private static final String REQUEST_PROCESSED_KEY = "request.processed";
    private static final String MOBILE_REQUEST_NOTE = "Requested via mobile";

    private final StudentRepository studentRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final OutboxPublisherService outboxPublisher;
    private final MessageResolver messages;

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
    public String solicitarEmprestimo(String matriculaAluno, String copyCode) {
        Student student = studentRepository.findByRegistrationNumber(matriculaAluno)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

        BookCopy bookCopy = bookCopyRepository.findByCopyCode(copyCode)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.copy.not-found"));

        long ativos = contarEmprestimosAtivos(matriculaAluno);
        RequestApprovalPolicy.validateRequest(student.getPenaltyExpiresAt(), ativos, bookCopy.getStatus());

        LoanRequest request = LoanRequest.builder()
                .student(student)
                .bookCopy(bookCopy)
                .build();
        loanRequestRepository.save(request);

        publicarEmailSolicitacao(student, bookCopy, "request.email.received");

        return REQUEST_CREATED_KEY;
    }

    @Transactional
    @CacheEvict(value = DASHBOARD_LOAN_REQUESTS, allEntries = true)
    public String solicitarEmprestimoPorLivro(String matriculaAluno, UUID bookId) {
        Student student = studentRepository.findByRegistrationNumber(matriculaAluno)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

        BookCopy bookCopy = bookCopyRepository.findFirstAvailable(bookId)
                .orElseThrow(() -> BusinessRuleException.ofKey("request.no-available-copy"));

        long ativos = contarEmprestimosAtivos(matriculaAluno);
        RequestApprovalPolicy.validateRequest(student.getPenaltyExpiresAt(), ativos, bookCopy.getStatus());

        LoanRequest request = LoanRequest.builder()
                .student(student)
                .bookCopy(bookCopy)
                .note(MOBILE_REQUEST_NOTE)
                .build();
        loanRequestRepository.save(request);

        publicarEmailSolicitacao(student, bookCopy, "request.email.received");

        return REQUEST_CREATED_KEY;
    }

    @Auditable(action = "REQUEST_PROCESSED", targetParam = "#id")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = DASHBOARD_LOAN_REQUESTS, allEntries = true),
            @CacheEvict(value = DASHBOARD_STATS, allEntries = true),
            @CacheEvict(value = DASHBOARD_OVERDUE_LIST, allEntries = true)
    })
    public String processarSolicitacao(UUID id, boolean aceitar) {
        LoanRequest request = loanRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("request.not-found"));

        Student student = request.getStudent();
        BookCopy bookCopy = request.getBookCopy();
        RequestApprovalPolicy.validateProcessable(request.getStatus());

        if (aceitar) {
            br.com.lumilivre.api.dto.loan.LoanRequest loanReq = br.com.lumilivre.api.dto.loan.LoanRequest.builder()
                    .studentRegistrationNumber(student.getRegistrationNumber())
                    .copyCode(bookCopy.getCopyCode())
                    .borrowedAt(OffsetDateTime.now())
                    .dueAt(OffsetDateTime.now().plusDays(14))
                    .build();
            loanService.cadastrar(loanReq);

            request.setStatus(LoanRequestStatus.ACCEPTED);
            loanRequestRepository.save(request);

            publicarEmailSolicitacao(student, bookCopy, "request.email.accepted");
        } else {
            request.setStatus(LoanRequestStatus.REJECTED);
            loanRequestRepository.save(request);

            publicarEmailSolicitacao(student, bookCopy, "request.email.rejected", EventType.REQUEST_REJECTED);
        }

        return REQUEST_PROCESSED_KEY;
    }

    private void publicarEmailSolicitacao(Student student, BookCopy bookCopy, String baseKey) {
        publicarEmailSolicitacao(student, bookCopy, baseKey, EventType.REQUEST_ACCEPTED);
    }

    private void publicarEmailSolicitacao(Student student, BookCopy bookCopy, String baseKey, EventType eventType) {
        Locale locale = localeFor(student);
        String subject = messages.resolve(baseKey + ".subject", locale);
        String body = messages.resolve(baseKey + ".body", locale, bookCopy.getBook().getTitle());
        outboxPublisher.publish(eventType, student.getEmail(), subject, body, locale);
    }

    private Locale localeFor(Student student) {
        if (student != null && student.getAppUser() != null
                && student.getAppUser().getPreferredLocale() != null
                && !student.getAppUser().getPreferredLocale().isBlank()) {
            return Locale.forLanguageTag(student.getAppUser().getPreferredLocale());
        }
        return Locale.forLanguageTag("pt-BR");
    }

    private long contarEmprestimosAtivos(String matricula) {
        return loanRepository.countByStudent_RegistrationNumberAndStatus(matricula, br.com.lumilivre.api.enums.LoanStatus.ACTIVE)
                + loanRepository.countByStudent_RegistrationNumberAndStatus(matricula, br.com.lumilivre.api.enums.LoanStatus.OVERDUE);
    }
}
