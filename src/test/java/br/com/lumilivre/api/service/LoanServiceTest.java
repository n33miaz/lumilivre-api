package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.LoanPolicy.LoanPolicyViolationException;
import br.com.lumilivre.api.dto.loan.LoanRequest;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.ReservationRepository;
import br.com.lumilivre.api.repository.ReaderRepository;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private ReaderRepository readerRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @Mock
    private MessageResolver messages;

    @InjectMocks
    private LoanService service;

    @Test
    void cadastrarRejectsDueDateBeforeBorrowDate() {
        LoanRequest request = request();
        request.setDueAt(request.getBorrowedAt().minusDays(1));

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("loan.return-date.before-borrow-date");

        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void cadastrarRejectsReaderAtActiveLoanLimit() {
        when(readerRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(reader()));
        when(loanRepository.countByReader_RegistrationNumberAndStatus("12345", LoanStatus.ACTIVE))
                .thenReturn((long) LoanStatus.values().length);
        when(loanRepository.countByReader_RegistrationNumberAndStatus("12345", LoanStatus.OVERDUE))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.cadastrar(request()))
                .isInstanceOf(LoanPolicyViolationException.class);

        verify(bookCopyRepository, never()).findByCopyCode(any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void cadastrarRejectsReaderWithActivePenalty() {
        Reader reader = reader();
        reader.setPenaltyExpiresAt(OffsetDateTime.now().plusDays(2));
        when(readerRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(reader));
        when(loanRepository.countByReader_RegistrationNumberAndStatus(any(), eq(LoanStatus.ACTIVE))).thenReturn(0L);
        when(loanRepository.countByReader_RegistrationNumberAndStatus(any(), eq(LoanStatus.OVERDUE))).thenReturn(0L);

        assertThatThrownBy(() -> service.cadastrar(request()))
                .isInstanceOf(LoanPolicyViolationException.class);

        verify(bookCopyRepository, never()).findByCopyCode(any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void cadastrarRejectsUnavailableBookCopy() {
        when(readerRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(reader()));
        when(loanRepository.countByReader_RegistrationNumberAndStatus(any(), eq(LoanStatus.ACTIVE))).thenReturn(0L);
        when(loanRepository.countByReader_RegistrationNumberAndStatus(any(), eq(LoanStatus.OVERDUE))).thenReturn(0L);
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(bookCopy(BookCopyStatus.BORROWED)));

        assertThatThrownBy(() -> service.cadastrar(request()))
                .isInstanceOf(BookAvailabilityViolationException.class);

        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void cadastrarCreatesActiveLoanMarksCopyBorrowedAndPublishesEmail() {
        Reader reader = reader();
        BookCopy copy = bookCopy(BookCopyStatus.AVAILABLE);
        when(readerRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(reader));
        when(loanRepository.countByReader_RegistrationNumberAndStatus("12345", LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.countByReader_RegistrationNumberAndStatus("12345", LoanStatus.OVERDUE)).thenReturn(0L);
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(copy));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan saved = service.cadastrar(request());

        assertThat(saved.getReader()).isSameAs(reader);
        assertThat(saved.getBookCopy()).isSameAs(copy);
        assertThat(saved.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.BORROWED);
        verify(bookCopyRepository).save(copy);
        verify(outboxPublisher).publish(eq(EventType.LOAN_CREATED), eq("leitor@lumilivre.test"), any(), any(), any());
    }

    @Test
    void cadastrarClearsExpiredPenaltyBeforeValidatingNewLoan() {
        Reader reader = reader();
        reader.setPenaltyCode(PenaltyCode.WARNING);
        reader.setPenaltyExpiresAt(OffsetDateTime.now().minusDays(1));
        when(readerRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(reader));
        when(loanRepository.countByReader_RegistrationNumberAndStatus("12345", LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.countByReader_RegistrationNumberAndStatus("12345", LoanStatus.OVERDUE)).thenReturn(0L);
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(bookCopy(BookCopyStatus.AVAILABLE)));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cadastrar(request());

        assertThat(reader.getPenaltyCode()).isNull();
        assertThat(reader.getPenaltyExpiresAt()).isNull();
        verify(readerRepository).save(reader);
    }

    @Test
    void atualizarUpdatesDatesForOpenLoan() {
        UUID id = UUID.randomUUID();
        Loan loan = loan(LoanStatus.ACTIVE, OffsetDateTime.now().plusDays(7));
        LoanRequest request = request();
        request.setBorrowedAt(OffsetDateTime.parse("2026-05-01T10:00:00-03:00"));
        request.setDueAt(OffsetDateTime.parse("2026-05-15T10:00:00-03:00"));
        when(loanRepository.findById(id)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan updated = service.atualizar(id, request);

        assertThat(updated.getBorrowedAt()).isEqualTo(OffsetDateTime.parse("2026-05-01T10:00:00-03:00"));
        assertThat(updated.getDueAt()).isEqualTo(OffsetDateTime.parse("2026-05-15T10:00:00-03:00"));
    }

    @Test
    void atualizarRejectsCompletedLoan() {
        UUID id = UUID.randomUUID();
        when(loanRepository.findById(id)).thenReturn(Optional.of(loan(LoanStatus.COMPLETED, OffsetDateTime.now())));

        assertThatThrownBy(() -> service.atualizar(id, request()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("loan.already-completed-cannot-update");
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void concluirEmprestimoCompletesLoanAndReleasesCopy() {
        UUID id = UUID.randomUUID();
        Loan loan = loan(LoanStatus.ACTIVE, OffsetDateTime.now().plusDays(1));
        when(loanRepository.findById(id)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(
                loan.getBookCopy().getBook().getId(), ReservationStatus.WAITING)).thenReturn(Optional.empty());

        Loan completed = service.concluirEmprestimo(id);

        assertThat(completed.getStatus()).isEqualTo(LoanStatus.COMPLETED);
        assertThat(completed.getReturnedAt()).isNotNull();
        assertThat(completed.getBookCopy().getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
        verify(bookCopyRepository).save(completed.getBookCopy());
        verify(outboxPublisher).publish(eq(EventType.LOAN_RETURNED), eq("leitor@lumilivre.test"), any(), any(), any());
    }

    @Test
    void concluirEmprestimoAppliesLatePenaltyAndPromotesNextReservation() {
        UUID id = UUID.randomUUID();
        Loan loan = loan(LoanStatus.ACTIVE, OffsetDateTime.now().minusDays(6));
        Reservation reservation = Reservation.builder()
                .reader(reader("54321", "next@lumilivre.test"))
                .book(loan.getBookCopy().getBook())
                .status(ReservationStatus.WAITING)
                .queuePosition(1)
                .build();
        when(loanRepository.findById(id)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        when(reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(
                loan.getBookCopy().getBook().getId(), ReservationStatus.WAITING)).thenReturn(Optional.of(reservation));

        service.concluirEmprestimo(id);

        assertThat(loan.getPenaltyCode()).isEqualTo(PenaltyCode.SUSPENSION);
        assertThat(loan.getReader().getPenaltyCode()).isEqualTo(PenaltyCode.SUSPENSION);
        assertThat(loan.getReader().getPenaltyExpiresAt()).isAfter(OffsetDateTime.now());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(reservation.getNotifiedAt()).isNotNull();
        assertThat(reservation.getExpiresAt()).isNotNull();
        verify(readerRepository).save(loan.getReader());
        verify(reservationRepository).save(reservation);
        verify(outboxPublisher).publish(eq(EventType.REQUEST_ACCEPTED), eq("next@lumilivre.test"), any(), any(), any());
    }

    @Test
    void excluirActiveLoanReleasesCopyBeforeDeletingLoan() {
        UUID id = UUID.randomUUID();
        Loan loan = loan(LoanStatus.ACTIVE, OffsetDateTime.now().plusDays(1));
        loan.getBookCopy().setStatus(BookCopyStatus.BORROWED);
        when(loanRepository.findById(id)).thenReturn(Optional.of(loan));

        service.excluir(id);

        assertThat(loan.getBookCopy().getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
        verify(bookCopyRepository).save(loan.getBookCopy());
        verify(loanRepository).deleteById(id);
    }

    @Test
    void renovarExtendsDueDateIncrementsCounterAndPublishesEmail() {
        UUID id = UUID.randomUUID();
        OffsetDateTime originalDueAt = OffsetDateTime.parse("2026-05-10T10:00:00-03:00");
        Loan loan = loan(LoanStatus.ACTIVE, originalDueAt);
        loan.setRenewalCount(1);
        when(loanRepository.findById(id)).thenReturn(Optional.of(loan));
        when(reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(
                loan.getBookCopy().getBook().getId(), ReservationStatus.WAITING)).thenReturn(Optional.empty());
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan renewed = service.renovar(id);

        assertThat(renewed.getDueAt()).isEqualTo(originalDueAt.plusDays(14));
        assertThat(renewed.getRenewalCount()).isEqualTo(2);
        assertThat(renewed.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        verify(outboxPublisher).publish(eq(EventType.REQUEST_ACCEPTED), eq("leitor@lumilivre.test"), any(), any(), any());
    }

    @Test
    void renovarRejectsWhenAnotherReaderIsFirstInReservationQueue() {
        UUID id = UUID.randomUUID();
        Loan loan = loan(LoanStatus.ACTIVE, OffsetDateTime.now().plusDays(7));
        Reservation reservation = Reservation.builder()
                .reader(reader("54321", "next@lumilivre.test"))
                .book(loan.getBookCopy().getBook())
                .status(ReservationStatus.WAITING)
                .queuePosition(1)
                .build();
        when(loanRepository.findById(id)).thenReturn(Optional.of(loan));
        when(reservationRepository.findFirstByBook_IdAndStatusOrderByQueuePositionAsc(
                loan.getBookCopy().getBook().getId(), ReservationStatus.WAITING)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.renovar(id))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessageContaining("loan.renewal.queued-reservation");
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void buscarPorTextoBlankDelegatesToAdvancedSearchWithStatusOrdering() {
        var pageable = PageRequest.of(0, 20, Sort.by("status").ascending());
        when(loanRepository.searchAdvancedListItems(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.buscarPorTexto(" ", pageable);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(loanRepository).searchAdvancedListItems(
                eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any(),
                captor.capture());
        Sort sort = captor.getValue().getSort();
        assertThat(sort.getOrderFor("status")).isNull();
        assertThat(sort.getOrderFor("dueAt")).isNotNull();
        assertThat(sort.toList()).hasSize(2);
    }

    @Test
    void overdueCountersCombineStoredOverdueAndActivePastDueLoans() {
        when(loanRepository.countByStatusIn(List.of(LoanStatus.OVERDUE))).thenReturn(2L);
        when(loanRepository.findByStatusAndDueAtBefore(eq(LoanStatus.ACTIVE), any()))
                .thenReturn(List.of(loan(LoanStatus.ACTIVE, OffsetDateTime.now().minusDays(1))));

        assertThat(service.getContagemAtrasadosReal()).isEqualTo(3);
    }

    private static LoanRequest request() {
        OffsetDateTime now = OffsetDateTime.now();
        return LoanRequest.builder()
                .readerRegistrationNumber("12345")
                .copyCode("T001")
                .borrowedAt(now)
                .dueAt(now.plusDays(14))
                .build();
    }

    private static Reader reader() {
        return reader("12345", "leitor@lumilivre.test");
    }

    private static Reader reader(String registrationNumber, String email) {
        Reader reader = new Reader();
        reader.setRegistrationNumber(registrationNumber);
        reader.setFullName("Leitor Teste");
        reader.setEmail(email);
        AppUser appUser = AppUser.builder()
                .email(email)
                .role(br.com.lumilivre.api.enums.Role.READER)
                .preferredLocale("en-US")
                .reader(reader)
                .build();
        reader.setAppUser(appUser);
        return reader;
    }

    private static BookCopy bookCopy(BookCopyStatus status) {
        Book book = new Book();
        book.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        book.setTitle("Livro Teste");

        BookCopy copy = new BookCopy();
        copy.setCopyCode("T001");
        copy.setStatus(status);
        copy.setBook(book);
        return copy;
    }

    private static Loan loan(LoanStatus status, OffsetDateTime dueAt) {
        return Loan.builder()
                .id(UUID.randomUUID())
                .reader(reader())
                .bookCopy(bookCopy(BookCopyStatus.BORROWED))
                .borrowedAt(dueAt.minusDays(14))
                .dueAt(dueAt)
                .status(status)
                .build();
    }
}
