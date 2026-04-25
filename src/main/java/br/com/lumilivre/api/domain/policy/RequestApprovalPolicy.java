package br.com.lumilivre.api.domain.policy;

import java.time.OffsetDateTime;

import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanRequestStatus;

public final class RequestApprovalPolicy {

    private RequestApprovalPolicy() {}

    public static void validateRequest(
            OffsetDateTime penaltyExpiry,
            long activeLoans,
            BookCopyStatus copyStatus) {

        if (penaltyExpiry != null && penaltyExpiry.isAfter(OffsetDateTime.now())) {
            throw new RequestApprovalViolationException("Student has an active penalty.");
        }

        if (activeLoans >= LoanPolicy.MAX_ACTIVE_LOANS) {
            throw new RequestApprovalViolationException("Student reached the active loan limit.");
        }

        BookAvailabilityPolicy.validateAvailable(copyStatus);
    }

    public static void validateProcessable(LoanRequestStatus currentStatus) {
        if (currentStatus != LoanRequestStatus.PENDING) {
            throw new RequestApprovalViolationException(
                    "Loan request is not pending. Current status: " + currentStatus);
        }
    }

    public static class RequestApprovalViolationException extends RuntimeException {
        public RequestApprovalViolationException(String message) {
            super(message);
        }
    }
}
