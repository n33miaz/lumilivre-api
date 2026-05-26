package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_ACTIVE_OVERDUE_COUNT;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_COUNT;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_OVERDUE_LIST;
import static br.com.lumilivre.api.config.CacheNames.DASHBOARD_STATS;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
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

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy;
import br.com.lumilivre.api.domain.policy.LoanPolicy;
import br.com.lumilivre.api.domain.policy.PenaltyPolicy;
import br.com.lumilivre.api.domain.policy.ReservationPolicy;
import br.com.lumilivre.api.dto.loan.ActiveLoanItem;
import br.com.lumilivre.api.dto.loan.LoanListItem;
import br.com.lumilivre.api.dto.loan.LoanRequest;
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

    private static final String STATUS_ORDER_EXPR =
            "(CASE WHEN e.status = 'COMPLETED' THEN 1 ELSE 0 END)";

    private final StudentRepository studentRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxPublisherService outboxPublisher;
    private final MessageResolver messages;

    @Auditable(action = "LOAN_CREATED", targetParam = "#request.studentRegistrationNumber")
    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_ACTIVE_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public Loan cadastrar(LoanRequest request) {
        if (request.getBorrowedAt() == null || request.getDueAt() == null) {
            throw BusinessRuleException.ofKey("loan.dates.required");
        }
        if (request.getDueAt().isBefore(request.getBorrowedAt())) {
            throw BusinessRuleException.ofKey("loan.return-date.before-borrow-date");
        }

        Student student = studentRepository.findByRegistrationNumber(request.getStudentRegistrationNumber())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

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

        BookCopy bookCopy = bookCopyRepository.findByCopyCode(request.getCopyCode())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.copy.not-found"));

        BookAvailabilityPolicy.validateAvailable(bookCopy.getStatus());

        Loan loan = Loan.builder()
                .student(student)
                .bookCopy(bookCopy)
                .borrowedAt(request.getBorrowedAt())
                .dueAt(request.getDueAt())
                .status(LoanStatus.ACTIVE)
                .build();

        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);

        Loan saved = loanRepository.save(loan);

        enviarEmailEmprestimo(student, bookCopy, request);

        return saved;
    }

    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_ACTIVE_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public Loan atualizar(UUID id, LoanRequest request) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("loan.not-found"));

        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw BusinessRuleException.ofKey("loan.already-completed-cannot-update");
        }

        loan.setBorrowedAt(request.getBorrowedAt());
        loan.setDueAt(request.getDueAt());

        return loanRepository.save(loan);
    }

    @Auditable(action = "LOAN_RETURNED", targetParam = "#id")
    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_ACTIVE_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public Loan concluirEmprestimo(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("loan.not-found"));

        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw BusinessRuleException.ofKey("loan.already-completed");
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
                    Locale recipientLocale = localeFor(next.getStudent());
                    outboxPublisher.publish(EventType.REQUEST_ACCEPTED,
                            next.getStudent().getEmail(),
                            messages.resolve("email.reservation-ready.subject", recipientLocale),
                            messages.resolve("email.reservation-ready.body", recipientLocale,
                                    bookCopy.getBook().getTitle(),
                                    next.getExpiresAt().toLocalDate()));
                });

        return saved;
    }

    @Transactional
    @CacheEvict(value = {
            DASHBOARD_STATS,
            DASHBOARD_ACTIVE_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public void excluir(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("loan.not-found"));

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
            DASHBOARD_ACTIVE_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_COUNT,
            DASHBOARD_OVERDUE_LIST
    }, allEntries = true)
    public Loan renovar(UUID id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("loan.not-found"));

        if (loan.getStatus() == LoanStatus.COMPLETED) {
            throw BusinessRuleException.ofKey("loan.renew.already-completed");
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

        Locale recipientLocale = localeFor(student);
        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, student.getEmail(),
                messages.resolve("email.loan-renewed.subject", recipientLocale),
                messages.resolve("email.loan-renewed.body", recipientLocale,
                        loan.getBookCopy().getBook().getTitle(),
                        saved.getDueAt().toLocalDate()));

        return saved;
    }

    public Page<LoanListItem> buscarEmprestimoParaListaAdminV2(Pageable pageable) {
        return loanRepository.findLoanListItems(tratarOrdenacao(pageable));
    }

    public Page<LoanListItem> buscarPorTexto(String texto, Pageable pageable) {
        Pageable pageableOrdenado = tratarOrdenacao(pageable);
        if (texto == null || texto.isBlank()) {
            return buscarAvancadoV2(null, null, null, null, null, null, (OffsetDateTime) null, pageableOrdenado);
        }
        return loanRepository.searchListItems(texto, pageableOrdenado);
    }

    public List<Loan> listarEmprestimosAlunoV2(String matricula) {
        return loanRepository.findActiveLoansForStudent(matricula);
    }

    public List<Loan> listarHistoricoV2(String matricula) {
        return loanRepository.findLoanHistoryForStudent(matricula);
    }

    public Loan buscarPorId(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("loan.not-found"));
    }

    public List<ActiveLoanItem> buscarAtivosEAtrasadosV2() {
        return loanRepository.findActiveAndOverdueItems();
    }

    @Cacheable(value = DASHBOARD_OVERDUE_COUNT)
    public long getContagemAtrasadosReal() {
        OffsetDateTime now = OffsetDateTime.now();
        return loanRepository.countByStatusIn(List.of(LoanStatus.OVERDUE))
                + loanRepository.findByStatusAndDueAtBefore(LoanStatus.ACTIVE, now).size();
    }

    @Cacheable(value = DASHBOARD_ACTIVE_OVERDUE_COUNT)
    public long getContagemEmprestimosAtivosEAtrasados() {
        return loanRepository.countByStatusIn(List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE));
    }

    public List<ActiveLoanItem> buscarApenasAtrasadosV2() {
        return loanRepository.findOverdueItems(
                LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
    }

    public Page<LoanListItem> buscarAvancadoV2(
            LoanStatus statusEmprestimo,
            String tombo,
            String livroNome,
            String alunoNomeCompleto,
            String dataEmprestimo,
            String dataDevolucao,
            String dataDevolucaoInicio,
            Pageable pageable) {
        OffsetDateTime dueAtStart = null;
        if (dataDevolucaoInicio != null && !dataDevolucaoInicio.isBlank()) {
            dueAtStart = LocalDate.parse(dataDevolucaoInicio)
                    .atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        }
        return buscarAvancadoV2(
                statusEmprestimo,
                tombo,
                livroNome,
                alunoNomeCompleto,
                dataEmprestimo,
                dataDevolucao,
                dueAtStart,
                pageable);
    }

    public Page<LoanListItem> buscarAvancadoV2(
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

        return loanRepository.searchAdvancedListItems(
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

    public List<Loan> buscarTodos() {
        return loanRepository.findAll();
    }

    private void enviarEmailEmprestimo(Student student, BookCopy bookCopy, LoanRequest request) {
        Locale locale = localeFor(student);
        String subject = messages.resolve("email.loan-created.subject", locale);
        String body = messages.resolve("email.loan-created.body", locale,
                student.getFullName(),
                bookCopy.getBook().getTitle(),
                request.getBorrowedAt(),
                request.getDueAt());
        outboxPublisher.publish(EventType.LOAN_CREATED, student.getEmail(), subject, body);
    }

    private void enviarEmailConclusao(Student student, BookCopy bookCopy, Loan loan) {
        Locale locale = localeFor(student);
        String penaltyStatus = loan.getPenaltyCode() != null
                ? loan.getPenaltyCode().getStatus()
                : messages.resolve("email.penalty.none", locale);
        String subject = messages.resolve("email.loan-completed.subject", locale);
        String body = messages.resolve("email.loan-completed.body", locale,
                student.getFullName(),
                bookCopy.getBook().getTitle(),
                penaltyStatus);
        outboxPublisher.publish(EventType.LOAN_RETURNED, student.getEmail(), subject, body);
    }

    private Locale localeFor(Student student) {
        if (student != null && student.getAppUser() != null
                && student.getAppUser().getPreferredLocale() != null
                && !student.getAppUser().getPreferredLocale().isBlank()) {
            return Locale.forLanguageTag(student.getAppUser().getPreferredLocale());
        }
        return Locale.forLanguageTag("pt-BR");
    }

    private Pageable tratarOrdenacao(Pageable pageable) {
        Sort.Order statusOrder = pageable.getSort().getOrderFor("status");
        if (statusOrder == null) {
            return pageable;
        }
        Sort sort = JpaSort.unsafe(Sort.Direction.ASC, STATUS_ORDER_EXPR)
                .and(JpaSort.unsafe(statusOrder.getDirection(), "dueAt"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
