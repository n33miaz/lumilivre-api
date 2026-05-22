package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.LoanRequestRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final MeterRegistry registry;
    private final LoanRepository loanRepository;
    private final LoanRequestRepository loanRequestRepository;

    @PostConstruct
    void registerGauges() {
        Gauge.builder("loans.active", loanRepository,
                        r -> r.countByStatusIn(List.of(LoanStatus.ACTIVE)))
                .description("Number of active loans")
                .register(registry);

        Gauge.builder("loans.overdue", loanRepository,
                        r -> r.countByStatusIn(List.of(LoanStatus.OVERDUE)))
                .description("Number of overdue loans")
                .register(registry);

        Gauge.builder("requests.pending", loanRequestRepository,
                        r -> r.countByStatus(LoanRequestStatus.PENDING))
                .description("Number of pending loan requests")
                .register(registry);

        Gauge.builder("returns.avg_days", loanRepository,
                        r -> {
                            Double avg = r.avgReturnDays();
                            return avg != null ? avg : 0.0;
                        })
                .description("Average days between borrow and return for completed loans")
                .register(registry);
    }
}
