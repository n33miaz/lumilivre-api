package br.com.lumilivre.api.domain.policy;

import java.time.OffsetDateTime;

import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.exception.custom.MessageKeyedException;

public final class RequestApprovalPolicy {

    private RequestApprovalPolicy() {}

    public static void validateRequest(
            OffsetDateTime penaltyExpiry,
            long activeLoans,
            BookCopyStatus copyStatus) {

        if (penaltyExpiry != null && penaltyExpiry.isAfter(OffsetDateTime.now())) {
            throw new RequestApprovalViolationException("request.policy.active-penalty", penaltyExpiry.toLocalDate());
        }

        if (activeLoans >= LoanPolicy.MAX_ACTIVE_LOANS) {
            throw new RequestApprovalViolationException(
                    "request.policy.active-loan-limit", LoanPolicy.MAX_ACTIVE_LOANS);
        }

        BookAvailabilityPolicy.validateAvailable(copyStatus);
    }

    public static void validateProcessable(LoanRequestStatus currentStatus) {
        if (currentStatus != LoanRequestStatus.PENDING) {
            throw new RequestApprovalViolationException("request.not-pending", currentStatus);
        }
    }

    public static class RequestApprovalViolationException extends RuntimeException implements MessageKeyedException {
        private final String messageKey;
        private final Object[] messageArgs;

        public RequestApprovalViolationException(String key, Object... args) {
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
