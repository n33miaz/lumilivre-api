package br.com.lumilivre.api.domain.policy;

import java.time.OffsetDateTime;

public final class LoanPolicy {

    public static final int MAX_ACTIVE_LOANS = 3;
    public static final int MAX_RENEWALS = 2;
    public static final int RENEWAL_DAYS = 14;

    private LoanPolicy() {}

    public static void validateNewLoan(long activeLoans, OffsetDateTime penaltyExpiry) {
        if (penaltyExpiry != null && penaltyExpiry.isAfter(OffsetDateTime.now())) {
            throw new LoanPolicyViolationException(
                    "Student has an active penalty until " + penaltyExpiry);
        }

        if (activeLoans >= MAX_ACTIVE_LOANS) {
            throw new LoanPolicyViolationException(
                    "Student has reached the limit of " + MAX_ACTIVE_LOANS + " active loans.");
        }
    }

    public static void validateRenewal(int currentRenewals, boolean hasQueuedReservation,
                                       OffsetDateTime penaltyExpiry) {
        if (penaltyExpiry != null && penaltyExpiry.isAfter(OffsetDateTime.now())) {
            throw new LoanPolicyViolationException(
                    "Student has an active penalty. Renewal blocked.");
        }
        if (hasQueuedReservation) {
            throw new LoanPolicyViolationException(
                    "Cannot renew: another student is waiting for this book in the reservation queue.");
        }
        if (currentRenewals >= MAX_RENEWALS) {
            throw new LoanPolicyViolationException(
                    "Renewal limit of " + MAX_RENEWALS + " reached for this loan.");
        }
    }

    public static class LoanPolicyViolationException extends RuntimeException {
        public LoanPolicyViolationException(String message) {
            super(message);
        }
    }
}
