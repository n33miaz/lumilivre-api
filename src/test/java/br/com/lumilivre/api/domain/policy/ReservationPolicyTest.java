package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;

class ReservationPolicyTest {

    @Test
    void validateNewReservationAllowsEligibleReader() {
        assertThatCode(() -> ReservationPolicy.validateNewReservation(null, 0, false))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNewReservationAllowsExpiredPenalty() {
        assertThatCode(() -> ReservationPolicy.validateNewReservation(
                OffsetDateTime.now().minusMinutes(1), 0, false))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNewReservationRejectsActivePenalty() {
        OffsetDateTime expiry = OffsetDateTime.now().plusDays(3);

        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(expiry, 0, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("reservation.policy.active-penalty");
    }

    @Test
    void validateNewReservationRejectsDuplicateReservationForSameBook() {
        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(null, 0, true))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("reservation.policy.already-active");
    }

    @Test
    void validateNewReservationRejectsBoundaryLimit() {
        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(
                null, ReservationPolicy.MAX_ACTIVE_RESERVATIONS, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("reservation.policy.limit-reached");
    }

    @Test
    void validateNewReservationRejectsAboveLimit() {
        assertThatThrownBy(() -> ReservationPolicy.validateNewReservation(
                null, ReservationPolicy.MAX_ACTIVE_RESERVATIONS + 1, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("reservation.policy.limit-reached");
    }

    @Test
    void calculatePickupDeadlineAddsToleranceDays() {
        OffsetDateTime notified = OffsetDateTime.parse("2026-04-10T09:00:00-03:00");

        OffsetDateTime deadline = ReservationPolicy.calculatePickupDeadline(notified);

        assertThat(deadline).isEqualTo(notified.plusDays(ReservationPolicy.PICKUP_DEADLINE_DAYS));
    }

    @Test
    void activeStatusesIncludesWaitingAndAvailable() {
        assertThat(ReservationPolicy.activeStatuses())
                .containsExactlyInAnyOrder(
                        ReservationStatus.WAITING,
                        ReservationStatus.READY)
                .doesNotContain(ReservationStatus.FULFILLED, ReservationStatus.CANCELLED, ReservationStatus.EXPIRED);
    }

    @Test
    void configuredLimitsMatchDocumentedContract() {
        assertThat(ReservationPolicy.MAX_ACTIVE_RESERVATIONS).isEqualTo(3);
        assertThat(ReservationPolicy.PICKUP_DEADLINE_DAYS).isEqualTo(2);
    }
}
