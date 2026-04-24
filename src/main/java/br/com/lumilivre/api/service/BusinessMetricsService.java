package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusSolicitacao;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.SolicitacaoEmprestimoRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final MeterRegistry registry;
    private final EmprestimoRepository emprestimoRepository;
    private final SolicitacaoEmprestimoRepository solicitacaoRepository;

    @PostConstruct
    void registerGauges() {
        Gauge.builder("loans.active", emprestimoRepository,
                        r -> r.countByStatusEmprestimoIn(List.of(StatusEmprestimo.ACTIVE)))
                .description("Número de empréstimos ativos")
                .register(registry);

        Gauge.builder("loans.overdue", emprestimoRepository,
                        r -> r.countByStatusEmprestimoIn(List.of(StatusEmprestimo.OVERDUE)))
                .description("Número de empréstimos atrasados")
                .register(registry);

        Gauge.builder("requests.pending", solicitacaoRepository,
                        r -> r.countByStatus(StatusSolicitacao.PENDING))
                .description("Número de solicitações pendentes")
                .register(registry);

        Gauge.builder("returns.avg_days", emprestimoRepository,
                        r -> {
                            Double avg = r.avgReturnDays();
                            return avg != null ? avg : 0.0;
                        })
                .description("Média de dias entre empréstimo e devolução (concluídos)")
                .register(registry);
    }
}
