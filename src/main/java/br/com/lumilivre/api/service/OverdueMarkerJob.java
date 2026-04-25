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
import br.com.lumilivre.api.repository.LoanRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OverdueMarkerJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueMarkerJob.class);

    private final LoanRepository loanRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void marcarAtrasados() {
        OffsetDateTime now = OffsetDateTime.now();

        List<Loan> overdue = loanRepository.findByStatusAndDueAtBefore(LoanStatus.ACTIVE, now);

        if (overdue.isEmpty()) {
            log.info("OverdueMarkerJob: no loans to mark as overdue.");
            return;
        }

        for (Loan loan : overdue) {
            loan.setStatus(LoanStatus.OVERDUE);
        }

        loanRepository.saveAll(overdue);
        log.info("OverdueMarkerJob: {} loan(s) marked as OVERDUE.", overdue.size());
    }
}
