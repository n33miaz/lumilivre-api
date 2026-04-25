package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_COUNT;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_LIST;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_STATS;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy;
import br.com.lumilivre.api.domain.policy.LoanPolicy;
import br.com.lumilivre.api.domain.policy.PenaltyPolicy;
import br.com.lumilivre.api.domain.policy.ReservationPolicy;
import br.com.lumilivre.api.dto.aluno.AlunoRankingResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoAtivoResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoDashboardResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoListagemResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.ReservationRepository;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.security.Auditable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final StudentRepository studentRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxPublisherService outboxPublisher;

    @Auditable(action = "LOAN_CREATED", targetParam = "#dto.aluno_matricula")
    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public EmprestimoResponse cadastrar(EmprestimoRequest dto) {
        if (dto.getData_emprestimo() == null || dto.getData_devolucao() == null) {
            throw new BusinessRuleException("Datas de empréstimo e devolução são obrigatórias.");
        }
        if (dto.getData_devolucao().isBefore(dto.getData_emprestimo())) {
            throw new BusinessRuleException("A data de devolução não pode ser anterior à data de empréstimo.");
        }

        Student student = studentRepository.findByRegistrationNumber(dto.getAluno_matricula())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado."));

        OffsetDateTime now = OffsetDateTime.now();
        if (student.getPenaltyExpiresAt() != null && student.getPenaltyExpiresAt().isBefore(now)) {
            student.setPenaltyCode(null);
            student.setPenaltyExpiresAt(null);
            studentRepository.save(student);
        }

        long activeLoans = loanRepository.countByStudent_RegistrationNumberAndStatus(
                student.getRegistrationNumber(), LoanStatus.ACTIVE)
                + loanRepository.countByStudent_RegistrationNumberAndStatus(
                        student.getRegistrationNumber(), LoanStatus.OVERDUE);

        LoanPolicy.validateNewLoan(activeLoans, student.getPenaltyExpiresAt());

        BookCopy bookCopy = bookCopyRepository.findByCopyCode(dto.getExemplar_tombo())
                .orElseThrow(() -> new ResourceNotFoundException("Exemplar não encontrado."));

        BookAvailabilityPolicy.validateAvailable(bookCopy.getStatus());

        Loan loan = Loan.builder()
                .student(student)
                .bookCopy(bookCopy)
                .borrowedAt(dto.getData_emprestimo())
                .dueAt(dto.getData_devolucao())
                .status(LoanStatus.ACTIVE)
                .build();

        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);

        Loan saved = loanRepository.save(loan);

        enviarEmailEmprestimo(student, bookCopy, dto);

        return new EmprestimoResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public EmprestimoResponse atualizar(EmprestimoRequest dto) {
        Loan loan = loanRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado."));

        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw new BusinessRuleException("Este empréstimo já foi concluído e não pode ser alterado.");
        }

        loan.setBorrowedAt(dto.getData_emprestimo());
        loan.setDueAt(dto.getData_devolucao());

        return new EmprestimoResponse(loanRepository.save(loan));
    }

    @Auditable(action = "LOAN_RETURNED", targetParam = "#id")
    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public EmprestimoResponse concluirEmprestimo(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado."));

        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw new BusinessRuleException("Este empréstimo já foi concluído.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Student student = loan.getStudent();

        if (loan.getDueAt().isBefore(now)) {
            long daysLate = Duration.between(loan.getDueAt(), now).toDays();
            PenaltyCode penalty = PenaltyPolicy.calculate(daysLate);
            loan.setPenaltyCode(penalty);

            if (PenaltyPolicy.isMoreSevere(penalty, student.getPenaltyCode())) {
                student.setPenaltyCode(penalty);
                student.setPenaltyExpiresAt(now.plusDays(7));
                studentRepository.save(student);
            }
        }

        loan.setStatus(LoanStatus.COMPLETED);
        loan.setReturnedAt(now);

        BookCopy bookCopy = loan.getBookCopy();
        bookCopy.setStatus(BookCopyStatus.AVAILABLE);
        bookCopyRepository.save(bookCopy);

        Loan saved = loanRepository.save(loan);

        enviarEmailConclusao(student, bookCopy, loan);

        reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(
                bookCopy.getBook().getId(), ReservationStatus.WAITING)
                .ifPresent(next -> {
                    next.setStatus(ReservationStatus.READY);
                    next.setNotifiedAt(now);
                    next.setExpiresAt(now.plusDays(ReservationPolicy.PICKUP_DEADLINE_DAYS));
                    reservationRepository.save(next);
                    outboxPublisher.publish(EventType.REQUEST_ACCEPTED,
                            next.getStudent().getEmail(),
                            "Livro disponível para retirada",
                            "O livro '" + bookCopy.getBook().getTitle() +
                            "' está disponível. Retire até " + next.getExpiresAt().toLocalDate() + ".");
                });

        return new EmprestimoResponse(saved);
    }

    @Transactional
    public void excluir(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado."));

        if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE) {
            BookCopy bookCopy = loan.getBookCopy();
            if (bookCopy != null) {
                bookCopy.setStatus(BookCopyStatus.AVAILABLE);
                bookCopyRepository.save(bookCopy);
            }
        }

        loanRepository.deleteById(id);
    }

    @Auditable(action = "LOAN_RENEWED", targetParam = "#id")
    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public EmprestimoResponse renovar(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empréstimo não encontrado."));

        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw new BusinessRuleException("Empréstimo já concluído não pode ser renovado.");
        }

        Student student = loan.getStudent();
        UUID bookId = loan.getBookCopy().getBook().getId();

        boolean hasReservation = reservationRepository
                .findFirstByBook_IdAndStatusOrderByQueuePositionAsc(bookId, ReservationStatus.WAITING)
                .map(r -> !r.getStudent().getRegistrationNumber().equals(student.getRegistrationNumber()))
                .orElse(false);

        LoanPolicy.validateRenewal(loan.getRenewalCount(), hasReservation, student.getPenaltyExpiresAt());

        loan.setDueAt(loan.getDueAt().plusDays(LoanPolicy.RENEWAL_DAYS));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loan.setStatus(LoanStatus.ACTIVE);

        Loan saved = loanRepository.save(loan);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, student.getEmail(),
                "Empréstimo renovado",
                "Seu empréstimo do livro '" + loan.getBookCopy().getBook().getTitle() +
                "' foi renovado. Nova data de devolução: " + saved.getDueAt().toLocalDate() + ".");

        return new EmprestimoResponse(saved);
    }

    public Page<EmprestimoListagemResponse> buscarEmprestimoParaListaAdmin(Pageable pageable) {
        return loanRepository.findEmprestimoParaListaAdmin(tratarOrdenacao(pageable));
    }

    public Page<EmprestimoListagemResponse> buscarPorTexto(String texto, Pageable pageable) {
        Pageable pageableNativo = pageable;
        Sort.Order statusOrder = pageable.getSort().getOrderFor("status");
        if (statusOrder != null) {
            boolean isAsc = statusOrder.isAscending();
            pageableNativo = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    isAsc
                    ? JpaSort.unsafe(Sort.Direction.ASC,
                            "(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END)", "dueAt")
                    : JpaSort.unsafe(Sort.Direction.ASC,
                            "(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END)")
                              .and(JpaSort.unsafe(Sort.Direction.DESC, "dueAt")));
        }
        if (texto == null || texto.isBlank()) {
            return buscarAvancado(null, null, null, null, null, null, null, tratarOrdenacao(pageable));
        }
        return loanRepository.buscarPorTexto(texto, pageableNativo);
    }

    public List<EmprestimoResponse> listarEmprestimosAluno(String matricula) {
        return loanRepository.findEmprestimosAtivos(matricula);
    }

    public List<EmprestimoResponse> listarHistorico(String matricula) {
        return loanRepository.findHistoricoEmprestimos(matricula);
    }

    @Cacheable(value = DASHBOARD_OVERDUE_LIST)
    public List<EmprestimoDashboardResponse> listarEmprestimosAtivosEAtrasados() {
        return loanRepository.findEmprestimosAtivosEAtrasados();
    }

    public List<EmprestimoAtivoResponse> buscarAtivosEAtrasados() {
        return loanRepository.findAtivosEAtrasadosDTO();
    }

    @Cacheable(value = DASHBOARD_OVERDUE_COUNT)
    public long getContagemAtrasadosReal() {
        OffsetDateTime now = OffsetDateTime.now();
        return loanRepository.countByStatusIn(List.of(LoanStatus.OVERDUE))
                + loanRepository.findByStatusAndDueAtBefore(LoanStatus.ACTIVE, now).size();
    }

    @Cacheable(value = DASHBOARD_STATS)
    public long getContagemEmprestimosAtivosEAtrasados() {
        return loanRepository.countByStatusIn(List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE));
    }

    public List<EmprestimoAtivoResponse> buscarApenasAtrasados() {
        return loanRepository.findApenasAtrasadosDTO(
                LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
    }

    public Page<EmprestimoListagemResponse> buscarAvancado(
            LoanStatus statusEmprestimo,
            String tombo,
            String livroNome,
            String alunoNomeCompleto,
            String dataEmprestimo,
            String dataDevolucao,
            OffsetDateTime dataDevolucaoInicio,
            Pageable pageable) {

        String tomboFiltro = (tombo != null && !tombo.isBlank()) ? "%" + tombo + "%" : null;
        String livroNomeFiltro = (livroNome != null && !livroNome.isBlank()) ? "%" + livroNome + "%" : null;
        String alunoNomeFiltro = (alunoNomeCompleto != null && !alunoNomeCompleto.isBlank())
                ? "%" + alunoNomeCompleto + "%" : null;

        OffsetDateTime dataEmprestimoInicio = null;
        if (dataEmprestimo != null && !dataEmprestimo.isBlank()) {
            dataEmprestimoInicio = LocalDate.parse(dataEmprestimo)
                    .atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        }

        OffsetDateTime dataDevolucaoFim = null;
        if (dataDevolucao != null && !dataDevolucao.isBlank()) {
            dataDevolucaoFim = LocalDate.parse(dataDevolucao)
                    .atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset());
        }

        String statusString = (statusEmprestimo != null) ? statusEmprestimo.name() : null;

        return loanRepository.buscarAvancado(
                statusString,
                tomboFiltro,
                livroNomeFiltro,
                alunoNomeFiltro,
                dataEmprestimoInicio,
                null,
                dataDevolucaoInicio,
                dataDevolucaoFim,
                OffsetDateTime.now(),
                tratarOrdenacao(pageable));
    }

    public List<AlunoRankingResponse> gerarRankingAlunos(int top, Integer cursoId, Integer moduloId, Integer turnoId) {
        return studentRepository.findRankingComFiltros(cursoId, moduloId, turnoId, PageRequest.of(0, top))
                .getContent();
    }

    public List<Loan> buscarTodos() {
        return loanRepository.findAll();
    }

    private void enviarEmailEmprestimo(Student student, BookCopy bookCopy, EmprestimoRequest dto) {
        String body = String.format(
                "Olá %s,\n\nSeu empréstimo do livro '%s' foi registrado com sucesso.\n" +
                "Data de empréstimo: %s\nData de devolução: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                student.getFullName(),
                bookCopy.getBook().getTitle(),
                dto.getData_emprestimo(),
                dto.getData_devolucao());
        outboxPublisher.publish(EventType.LOAN_CREATED, student.getEmail(), "Empréstimo registrado", body);
    }

    private void enviarEmailConclusao(Student student, BookCopy bookCopy, Loan loan) {
        String body = String.format(
                "Olá %s,\n\nSeu empréstimo do livro '%s' foi concluído.\n" +
                "Status da penalidade: %s\n\nAtenciosamente,\nBiblioteca LumiLivre",
                student.getFullName(),
                bookCopy.getBook().getTitle(),
                loan.getPenaltyCode() != null ? loan.getPenaltyCode().getStatus() : "Nenhuma");
        outboxPublisher.publish(EventType.LOAN_RETURNED, student.getEmail(), "Empréstimo concluído", body);
    }

    private Pageable tratarOrdenacao(Pageable pageable) {
        Sort.Order statusOrder = pageable.getSort().getOrderFor("status");
        if (statusOrder != null) {
            boolean isAsc = statusOrder.isAscending();
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    isAsc
                    ? Sort.by(Sort.Direction.ASC, "ordemStatus", "dueAt")
                    : Sort.by(Sort.Order.asc("ordemStatus"), Sort.Order.desc("dueAt")));
        }
        return pageable;
    }
}
