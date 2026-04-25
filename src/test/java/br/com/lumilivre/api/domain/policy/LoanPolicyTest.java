package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.domain.policy.LoanPolicy.LoanPolicyViolationException;

class LoanPolicyTest {

    @Test
    void validateNewLoanAllowsStudentWithoutActivePenaltyBelowLimit() {
        assertThatCode(() -> LoanPolicy.validateNewLoan(LoanPolicy.MAX_ACTIVE_LOANS - 1, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNewLoanAllowsExpiredPenalty() {
        assertThatCode(() -> LoanPolicy.validateNewLoan(0, OffsetDateTime.now().minusMinutes(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateNewLoanRejectsActivePenalty() {
        assertThatThrownBy(() -> LoanPolicy.validateNewLoan(0, OffsetDateTime.now().plusMinutes(1)))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessageContaining("active penalty");
    }

    @Test
    void validateNewLoanRejectsLimitBoundary() {
        assertThatThrownBy(() -> LoanPolicy.validateNewLoan(LoanPolicy.MAX_ACTIVE_LOANS, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void validateNewLoanRejectsAboveLimit() {
        assertThatThrownBy(() -> LoanPolicy.validateNewLoan(LoanPolicy.MAX_ACTIVE_LOANS + 1, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void renewalPolicyMatchesPlannedLimits() {
        assertThat(LoanPolicy.MAX_RENEWALS).isEqualTo(2);
        assertThat(LoanPolicy.RENEWAL_DAYS).isEqualTo(14);
    }

    @Test
    void validateRenewalAllowsUntilLimitBoundary() {
        assertThatCode(() -> LoanPolicy.validateRenewal(LoanPolicy.MAX_RENEWALS - 1, false, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRenewalRejectsUsedLimit() {
        assertThatThrownBy(() -> LoanPolicy.validateRenewal(LoanPolicy.MAX_RENEWALS, false, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void validateRenewalRejectsQueuedReservationFromOtherStudent() {
        assertThatThrownBy(() -> LoanPolicy.validateRenewal(0, true, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessageContaining("reservation queue");
    }
}
