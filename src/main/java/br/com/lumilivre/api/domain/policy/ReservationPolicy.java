package br.com.lumilivre.api.domain.policy;

import java.time.OffsetDateTime;
import java.util.List;

import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;

public final class ReservationPolicy {

    public static final int PICKUP_DEADLINE_DAYS = 2;
    public static final int MAX_ACTIVE_RESERVATIONS = 3;

    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private ReservationPolicy() {}

    public static void validateNewReservation(
            OffsetDateTime penaltyExpiry,
            long activeReservations,
            boolean alreadyReserved) {

        if (penaltyExpiry != null && penaltyExpiry.isAfter(OffsetDateTime.now())) {
            throw new BusinessRuleException(
                    "Student has an active penalty until " + penaltyExpiry.toLocalDate() +
                    ". Reservations are blocked.");
        }
        if (alreadyReserved) {
            throw new BusinessRuleException("Student already has an active reservation for this book.");
        }
        if (activeReservations >= MAX_ACTIVE_RESERVATIONS) {
            throw new BusinessRuleException(
                    "Limit of " + MAX_ACTIVE_RESERVATIONS + " simultaneous reservations reached.");
        }
    }

    public static OffsetDateTime calculatePickupDeadline(OffsetDateTime notifiedAt) {
        return notifiedAt.plusDays(PICKUP_DEADLINE_DAYS);
    }

    public static List<ReservationStatus> activeStatuses() {
        return ACTIVE_STATUSES;
    }
}
