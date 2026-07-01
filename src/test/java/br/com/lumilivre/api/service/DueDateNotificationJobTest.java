package br.com.lumilivre.api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.service.infra.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DueDateNotificationJobTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private MessageResolver messages;

    @InjectMocks
    private DueDateNotificationJob job;

    @Test
    void enviarLembretesSendsThreeDayReminderWithReaderPreferredLocale() {
        Loan dueInThreeDays = loan(LoanStatus.ACTIVE, OffsetDateTime.now().plusDays(3), "en-US");
        when(loanRepository.findByStatusAndDueAtGreaterThanEqual(eq(LoanStatus.ACTIVE), any(OffsetDateTime.class)))
                .thenReturn(List.of(dueInThreeDays))
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(loanRepository.findByStatus(LoanStatus.OVERDUE)).thenReturn(List.of());

        job.enviarLembretes();

        verify(emailService).enviarNotificacaoEmprestimo(
                "reader@example.test",
                "email.loan-due-3days.subject",
                "email.loan-due-3days.body",
                "Clean Architecture",
                Locale.forLanguageTag("en-US"));
    }

    @Test
    void enviarLembretesSendsOverdueNotificationWithDefaultLocaleWhenReaderHasNoUser() {
        Loan overdue = loan(LoanStatus.OVERDUE, OffsetDateTime.now().minusDays(2), null);
        when(loanRepository.findByStatusAndDueAtGreaterThanEqual(eq(LoanStatus.ACTIVE), any(OffsetDateTime.class)))
                .thenReturn(List.of())
                .thenReturn(List.of())
                .thenReturn(List.of());
        when(loanRepository.findByStatus(LoanStatus.OVERDUE)).thenReturn(List.of(overdue));

        job.enviarLembretes();

        verify(emailService).enviarNotificacaoEmprestimo(
                "reader@example.test",
                "email.loan-overdue.subject",
                "email.loan-overdue.body",
                "Clean Architecture",
                Locale.forLanguageTag("pt-BR"));
    }

    private static Loan loan(LoanStatus status, OffsetDateTime dueAt, String preferredLocale) {
        Reader reader = new Reader();
        reader.setEmail("reader@example.test");
        if (preferredLocale != null) {
            AppUser user = new AppUser();
            user.setPreferredLocale(preferredLocale);
            reader.setAppUser(user);
        }

        Book book = new Book();
        book.setTitle("Clean Architecture");
        BookCopy copy = new BookCopy();
        copy.setBook(book);

        return Loan.builder()
                .status(status)
                .reader(reader)
                .bookCopy(copy)
                .borrowedAt(dueAt.minusDays(14))
                .dueAt(dueAt)
                .build();
    }
}
