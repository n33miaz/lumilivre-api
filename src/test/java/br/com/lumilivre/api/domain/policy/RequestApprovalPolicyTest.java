package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy.RequestApprovalViolationException;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanRequestStatus;

class RequestApprovalPolicyTest {

    @Test
    void validateRequestAllowsValidRequest() {
        assertThatCode(() -> RequestApprovalPolicy.validateRequest(
                null,
                LoanPolicy.MAX_ACTIVE_LOANS - 1,
                BookCopyStatus.AVAILABLE))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRequestAllowsExpiredPenalty() {
        assertThatCode(() -> RequestApprovalPolicy.validateRequest(
                OffsetDateTime.now().minusMinutes(1),
                0,
                BookCopyStatus.AVAILABLE))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRequestRejectsActivePenalty() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateRequest(
                OffsetDateTime.now().plusMinutes(1),
                0,
                BookCopyStatus.AVAILABLE))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessageContaining("active penalty");
    }

    @Test
    void validateRequestRejectsLoanLimitBoundary() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateRequest(
                null,
                LoanPolicy.MAX_ACTIVE_LOANS,
                BookCopyStatus.AVAILABLE))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessageContaining("active loan limit");
    }

    @Test
    void validateRequestRejectsUnavailableBook() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateRequest(
                null,
                0,
                BookCopyStatus.BORROWED))
                .isInstanceOf(BookAvailabilityViolationException.class);
    }

    @Test
    void validateProcessableAllowsPendingRequest() {
        assertThatCode(() -> RequestApprovalPolicy.validateProcessable(LoanRequestStatus.PENDING))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = LoanRequestStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void validateProcessableRejectsAlreadyProcessedRequests(LoanRequestStatus status) {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateProcessable(status))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void validateProcessableRejectsNullStatus() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateProcessable(null))
                .isInstanceOf(RequestApprovalViolationException.class);
    }
}
