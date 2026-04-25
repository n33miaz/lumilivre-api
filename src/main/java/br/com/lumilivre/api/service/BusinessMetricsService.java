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
                .description("Número de empréstimos ativos")
                .register(registry);

        Gauge.builder("loans.overdue", loanRepository,
                        r -> r.countByStatusIn(List.of(LoanStatus.OVERDUE)))
                .description("Número de empréstimos atrasados")
                .register(registry);

        Gauge.builder("requests.pending", loanRequestRepository,
                        r -> r.countByStatus(LoanRequestStatus.PENDING))
                .description("Número de solicitações pendentes")
                .register(registry);

        Gauge.builder("returns.avg_days", loanRepository,
                        r -> {
                            Double avg = r.avgReturnDays();
                            return avg != null ? avg : 0.0;
                        })
                .description("Média de dias entre empréstimo e devolução (concluídos)")
                .register(registry);
    }
}
