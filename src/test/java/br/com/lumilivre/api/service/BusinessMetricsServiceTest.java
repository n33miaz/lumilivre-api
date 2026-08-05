package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.model.OutboxEvent.EventStatus;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.LoanRequestRepository;
import br.com.lumilivre.api.repository.OutboxEventRepository;
import br.com.lumilivre.api.repository.ReservationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanRequestRepository loanRequestRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void registerGaugesPublishesCurrentRepositoryValues() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(loanRepository.countByStatusIn(List.of(LoanStatus.ACTIVE))).thenReturn(4L);
        when(loanRepository.countByStatusIn(List.of(LoanStatus.OVERDUE))).thenReturn(1L);
        when(loanRequestRepository.countByStatus(LoanRequestStatus.PENDING)).thenReturn(2L);
        when(loanRepository.avgReturnDays()).thenReturn(6.5);
        when(reservationRepository.countByStatus(ReservationStatus.WAITING)).thenReturn(3L);
        when(reservationRepository.countByStatus(ReservationStatus.EXPIRED)).thenReturn(1L);

        newService(registry).registerGauges();

        assertThat(registry.get("loans.active").gauge().value()).isEqualTo(4.0);
        assertThat(registry.get("loans.overdue").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("requests.pending").gauge().value()).isEqualTo(2.0);
        assertThat(registry.get("returns.avg_days").gauge().value()).isEqualTo(6.5);
        assertThat(registry.get("reservations.queued").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("reservations.expired").gauge().value()).isEqualTo(1.0);
    }

    @Test
    void averageReturnGaugeFallsBackToZeroWhenRepositoryReturnsNull() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(loanRepository.avgReturnDays()).thenReturn(null);

        newService(registry).registerGauges();

        assertThat(registry.get("returns.avg_days").gauge().value()).isZero();
    }

    @Test
    void outboxGaugesExposeBacklogSizeAndFailureCount() {
        // O alerta OutboxBacklogHigh apontava para uma série que não existia:
        // o outbox parava de drenar e nenhum painel mostrava.
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(outboxEventRepository.countByStatus(EventStatus.PENDING)).thenReturn(7L);
        when(outboxEventRepository.countByStatus(EventStatus.FAILED)).thenReturn(2L);

        newService(registry).registerGauges();

        assertThat(registry.get("outbox.pending").gauge().value()).isEqualTo(7.0);
        assertThat(registry.get("outbox.failed").gauge().value()).isEqualTo(2.0);
    }

    @Test
    void oldestPendingAgeMeasuresHowLongTheQueueHasBeenStuck() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(outboxEventRepository.findOldestCreatedAtByStatus(EventStatus.PENDING))
                .thenReturn(OffsetDateTime.now().minusMinutes(10));

        newService(registry).registerGauges();

        // Aproximação: o teste mede o intervalo em execução, não um instante fixo.
        assertThat(registry.get("outbox.oldest_pending_age_seconds").gauge().value())
                .isBetween(590.0, 610.0);
    }

    @Test
    void oldestPendingAgeIsZeroWhenTheQueueIsEmpty() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        when(outboxEventRepository.findOldestCreatedAtByStatus(EventStatus.PENDING)).thenReturn(null);

        newService(registry).registerGauges();

        assertThat(registry.get("outbox.oldest_pending_age_seconds").gauge().value()).isZero();
    }

    /**
     * Todos os gauges são registrados sempre, mas cada teste só afirma sobre os
     * seus. Os defaults lenient evitam que os gauges alheios estourem por falta
     * de stub — e ficam antes das expectativas de cada teste para que a
     * dublagem específica prevaleça.
     */
    @BeforeEach
    void defaultCounts() {
        lenient().when(loanRepository.countByStatusIn(List.of(LoanStatus.ACTIVE))).thenReturn(0L);
        lenient().when(loanRepository.countByStatusIn(List.of(LoanStatus.OVERDUE))).thenReturn(0L);
        lenient().when(loanRequestRepository.countByStatus(LoanRequestStatus.PENDING)).thenReturn(0L);
        lenient().when(loanRepository.avgReturnDays()).thenReturn(0.0);
        lenient().when(reservationRepository.countByStatus(ReservationStatus.WAITING)).thenReturn(0L);
        lenient().when(reservationRepository.countByStatus(ReservationStatus.EXPIRED)).thenReturn(0L);
        lenient().when(outboxEventRepository.countByStatus(EventStatus.PENDING)).thenReturn(0L);
        lenient().when(outboxEventRepository.countByStatus(EventStatus.FAILED)).thenReturn(0L);
        lenient().when(outboxEventRepository.findOldestCreatedAtByStatus(EventStatus.PENDING)).thenReturn(null);
    }

    private BusinessMetricsService newService(SimpleMeterRegistry registry) {
        return new BusinessMetricsService(registry, loanRepository, loanRequestRepository,
                reservationRepository, outboxEventRepository);
    }
}
