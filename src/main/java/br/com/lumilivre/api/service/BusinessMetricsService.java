package br.com.lumilivre.api.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.model.OutboxEvent.EventStatus;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.LoanRequestRepository;
import br.com.lumilivre.api.repository.OutboxEventRepository;
import br.com.lumilivre.api.repository.ReservationRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Gauges de negócio publicados em {@code /actuator/prometheus}.
 *
 * <p>O critério para estar aqui é operacional, não estatístico: cada medida
 * precisa responder a uma pergunta que alguém faz durante um incidente ou uma
 * degradação silenciosa. Contagem que só interessa a relatório vive no dashboard
 * de gestão, que lê o banco.
 */
@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final MeterRegistry registry;
    private final LoanRepository loanRepository;
    private final LoanRequestRepository loanRequestRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;

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

        // Fila de reservas: é o que a biblioteca "deve" aos leitores. Subindo sem
        // parar, ou há falta de exemplar ou o job de conversão parou.
        Gauge.builder("reservations.queued", reservationRepository,
                        r -> r.countByStatus(ReservationStatus.WAITING))
                .description("Reservations waiting in queue")
                .register(registry);

        // Reserva expirada é livro que ficou separado no balcão e ninguém buscou.
        // Crescimento anormal costuma ser aviso de e-mail que não está chegando —
        // o leitor não soube que podia retirar.
        Gauge.builder("reservations.expired", reservationRepository,
                        r -> r.countByStatus(ReservationStatus.EXPIRED))
                .description("Reservations that expired before pickup")
                .register(registry);

        // ---------------------------------------------------------------
        // Outbox: a única falha do sistema que é completamente silenciosa.
        // Se o publisher para (SMTP fora, exceção no scheduler, scheduling
        // desligado por engano), nada quebra para o usuário — os e-mails
        // simplesmente deixam de sair, e isso só aparecia como linha de log.
        // ---------------------------------------------------------------
        Gauge.builder("outbox.pending", outboxEventRepository,
                        r -> r.countByStatus(EventStatus.PENDING))
                .description("Outbox events waiting to be delivered")
                .register(registry);

        // Contagem sozinha não distingue "cinco criados agora" de "cinco parados
        // há três dias". A idade é o sinal de que a fila deixou de drenar.
        Gauge.builder("outbox.oldest_pending_age_seconds", outboxEventRepository,
                        BusinessMetricsService::oldestPendingAgeSeconds)
                .description("Age in seconds of the oldest pending outbox event")
                .register(registry);

        // Esgotou os retries: não sai mais sozinho, precisa de gente.
        Gauge.builder("outbox.failed", outboxEventRepository,
                        r -> r.countByStatus(EventStatus.FAILED))
                .description("Outbox events abandoned after exhausting retries")
                .register(registry);
    }

    private static double oldestPendingAgeSeconds(OutboxEventRepository repository) {
        OffsetDateTime oldest = repository.findOldestCreatedAtByStatus(EventStatus.PENDING);
        if (oldest == null) {
            return 0.0;
        }
        return Math.max(0, Duration.between(oldest, OffsetDateTime.now()).toSeconds());
    }
}
