package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.service.infra.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DueDateNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(DueDateNotificationJob.class);

    private final LoanRepository loanRepository;
    private final EmailService emailService;
    private final MessageResolver messages;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void enviarLembretes() {
        OffsetDateTime now = OffsetDateTime.now();

        notificarWindowDias(now, 3, "email.loan-due-3days.subject", "email.loan-due-3days.body");
        notificarWindowDias(now, 1, "email.loan-due-1day.subject", "email.loan-due-1day.body");
        notificarWindowDias(now, 0, "email.loan-due-today.subject", "email.loan-due-today.body");
        notificarAtrasados();
    }

    private void notificarWindowDias(OffsetDateTime now, int daysAhead, String subjectKey, String bodyKey) {
        OffsetDateTime start = now.plusDays(daysAhead).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime end = start.plusDays(1);

        List<Loan> vencendo = loanRepository
                .findByStatusAndDueAtGreaterThanEqual(LoanStatus.ACTIVE, start)
                .stream()
                .filter(e -> e.getDueAt().isBefore(end))
                .toList();

        for (Loan loan : vencendo) {
            String bookTitle = loan.getBookCopy().getBook().getTitle();
            Locale locale = resolveStudentLocale(loan);
            emailService.enviarNotificacaoEmprestimo(
                    loan.getStudent().getEmail(), subjectKey, bodyKey, bookTitle, locale);
        }

        if (!vencendo.isEmpty()) {
            log.info("DueDateNotificationJob: {} reminder(s) D-{} sent.", vencendo.size(), daysAhead);
        }
    }

    private void notificarAtrasados() {
        List<Loan> overdue = loanRepository.findByStatus(LoanStatus.OVERDUE);

        for (Loan loan : overdue) {
            String bookTitle = loan.getBookCopy().getBook().getTitle();
            Locale locale = resolveStudentLocale(loan);
            emailService.enviarNotificacaoEmprestimo(
                    loan.getStudent().getEmail(),
                    "email.loan-overdue.subject",
                    "email.loan-overdue.body",
                    bookTitle, locale);
        }

        if (!overdue.isEmpty()) {
            log.info("DueDateNotificationJob: {} overdue notification(s) sent.", overdue.size());
        }
    }

    private Locale resolveStudentLocale(Loan loan) {
        try {
            String tag = loan.getStudent().getAppUser().getPreferredLocale();
            return tag != null ? Locale.forLanguageTag(tag) : Locale.forLanguageTag("pt-BR");
        } catch (Exception e) {
            return Locale.forLanguageTag("pt-BR");
        }
    }
}
