package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.enums.StatusReserva;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;

class ReservationPolicyTest {

    @Test
    void validateNewReservationAllowsEligibleStudent() {
        assertThatCode(() -> ReservationPolicy.validateNewReservation(null, 0, false))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNewReservationAllowsExpiredPenalty() {
        assertThatCode(() -> ReservationPolicy.validateNewReservation(
                LocalDateTime.now().minusMinutes(1), 0, false))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNewReservationRejectsActivePenalty() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(3);

        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(expiry, 0, false))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("penalidade ativa");
    }

    @Test
    void validateNewReservationRejectsDuplicateReservationForSameBook() {
        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(null, 0, true))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("reserva ativa para este livro");
    }

    @Test
    void validateNewReservationRejectsBoundaryLimit() {
        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(
                null, ReservationPolicy.MAX_ACTIVE_RESERVATIONS, false))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Limite");
    }

    @Test
    void validateNewReservationRejectsAboveLimit() {
        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(
                null, ReservationPolicy.MAX_ACTIVE_RESERVATIONS + 1, false))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Limite");
    }

    @Test
    void calculatePickupDeadlineAddsToleranceDays() {
        LocalDateTime notified = LocalDateTime.of(2026, 4, 10, 9, 0);

        LocalDateTime deadline = ReservationPolicy.calculatePickupDeadline(notified);

        assertThat(deadline).isEqualTo(notified.plusDays(ReservationPolicy.PICKUP_DEADLINE_DAYS));
    }

    @Test
    void activeStatusesIncludesWaitingAndAvailable() {
        assertThat(ReservationPolicy.activeStatuses())
                .containsExactlyInAnyOrder(
                        StatusReserva.WAITING,
                        StatusReserva.READY)
                .doesNotContain(StatusReserva.FULFILLED, StatusReserva.CANCELLED, StatusReserva.EXPIRED);
    }

    @Test
    void configuredLimitsMatchDocumentedContract() {
        assertThat(ReservationPolicy.MAX_ACTIVE_RESERVATIONS).isEqualTo(3);
        assertThat(ReservationPolicy.PICKUP_DEADLINE_DAYS).isEqualTo(2);
    }
}
