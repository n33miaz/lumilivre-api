package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.repository.LoanRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DueDateNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(DueDateNotificationJob.class);

    private final LoanRepository loanRepository;
    private final OutboxPublisherService outboxPublisher;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void enviarLembretes() {
        OffsetDateTime now = OffsetDateTime.now();

        notificarWindowDias(now, 3, "Lembrete: devolução em 3 dias",
                "Você tem 3 dias para devolver o livro '%s'.");
        notificarWindowDias(now, 1, "Lembrete: devolução amanhã",
                "Seu empréstimo do livro '%s' vence amanhã.");
        notificarWindowDias(now, 0, "Devolução hoje",
                "O prazo de devolução do livro '%s' é hoje.");
        notificarAtrasados();
    }

    private void notificarWindowDias(OffsetDateTime now, int daysAhead, String subject, String template) {
        OffsetDateTime start = now.plusDays(daysAhead).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime end = start.plusDays(1);

        List<Loan> vencendo = loanRepository
                .findByStatusAndDueAtGreaterThanEqual(LoanStatus.ACTIVE, start)
                .stream()
                .filter(e -> e.getDueAt().isBefore(end))
                .toList();

        for (Loan loan : vencendo) {
            String bookTitle = loan.getBookCopy().getBook().getTitle();
            outboxPublisher.publish(EventType.REQUEST_ACCEPTED,
                    loan.getStudent().getEmail(),
                    subject,
                    String.format(template, bookTitle));
        }

        if (!vencendo.isEmpty()) {
            log.info("DueDateNotificationJob: {} reminder(s) D-{} sent.", vencendo.size(), daysAhead);
        }
    }

    private void notificarAtrasados() {
        List<Loan> overdue = loanRepository.findByStatus(LoanStatus.OVERDUE);

        for (Loan loan : overdue) {
            String bookTitle = loan.getBookCopy().getBook().getTitle();
            outboxPublisher.publish(EventType.REQUEST_REJECTED,
                    loan.getStudent().getEmail(),
                    "Empréstimo em atraso",
                    "Seu empréstimo do livro '" + bookTitle + "' está em atraso. Regularize o quanto antes.");
        }

        if (!overdue.isEmpty()) {
            log.info("DueDateNotificationJob: {} overdue notification(s) sent.", overdue.size());
        }
    }
}
