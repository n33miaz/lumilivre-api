package br.com.lumilivre.api.domain.policy;

import java.time.OffsetDateTime;

import br.com.lumilivre.api.exception.custom.MessageKeyedException;

public final class LoanPolicy {

    public static final int MAX_ACTIVE_LOANS = 3;
    public static final int MAX_RENEWALS = 2;
    public static final int RENEWAL_DAYS = 14;

    private LoanPolicy() {}

    public static void validateNewLoan(long activeLoans, OffsetDateTime penaltyExpiry) {
        if (penaltyExpiry != null && penaltyExpiry.isAfter(OffsetDateTime.now())) {
            throw new LoanPolicyViolationException(
                    "loan.policy-violation.active-penalty", penaltyExpiry.toLocalDate());
        }

        if (activeLoans >= MAX_ACTIVE_LOANS) {
            throw new LoanPolicyViolationException(
                    "loan.policy-violation.max-active-loans-reached", MAX_ACTIVE_LOANS);
        }
    }

    public static void validateRenewal(int currentRenewals, boolean hasQueuedReservation,
                                       OffsetDateTime penaltyExpiry) {
        if (penaltyExpiry != null && penaltyExpiry.isAfter(OffsetDateTime.now())) {
            throw new LoanPolicyViolationException(
                    "loan.renewal.active-penalty", penaltyExpiry.toLocalDate());
        }
        if (hasQueuedReservation) {
            throw new LoanPolicyViolationException("loan.renewal.queued-reservation");
        }
        if (currentRenewals >= MAX_RENEWALS) {
            throw new LoanPolicyViolationException("loan.renewal.limit-reached", MAX_RENEWALS);
        }
    }

    public static class LoanPolicyViolationException extends RuntimeException implements MessageKeyedException {
        private final String messageKey;
        private final Object[] messageArgs;

        public LoanPolicyViolationException(String key, Object... args) {
            super(key);
            this.messageKey = key;
            this.messageArgs = args;
        }

        @Override
        public boolean hasI18nKey() {
            return true;
        }

        @Override
        public String getMessageKey() {
            return messageKey;
        }

        @Override
        public Object[] getMessageArgs() {
            return messageArgs;
        }
    }
}
