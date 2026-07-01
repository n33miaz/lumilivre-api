package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.domain.policy.LoanPolicy.LoanPolicyViolationException;

class LoanPolicyTest {

    @Test
    void validateNewLoanAllowsReaderWithoutActivePenaltyBelowLimit() {
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
                .hasMessage("loan.policy-violation.active-penalty");
    }

    @Test
    void validateNewLoanRejectsLimitBoundary() {
        assertThatThrownBy(() -> LoanPolicy.validateNewLoan(LoanPolicy.MAX_ACTIVE_LOANS, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessage("loan.policy-violation.max-active-loans-reached");
    }

    @Test
    void validateNewLoanRejectsAboveLimit() {
        assertThatThrownBy(() -> LoanPolicy.validateNewLoan(LoanPolicy.MAX_ACTIVE_LOANS + 1, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessage("loan.policy-violation.max-active-loans-reached");
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
    void validateRenewalAllowsExpiredPenalty() {
        assertThatCode(() -> LoanPolicy.validateRenewal(0, false, OffsetDateTime.now().minusMinutes(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRenewalRejectsActivePenalty() {
        OffsetDateTime penaltyExpiry = OffsetDateTime.now().plusDays(2);

        assertThatThrownBy(() -> LoanPolicy.validateRenewal(0, false, penaltyExpiry))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessage("loan.renewal.active-penalty")
                .satisfies(error -> {
                    LoanPolicyViolationException violation = (LoanPolicyViolationException) error;
                    assertThat(violation.hasI18nKey()).isTrue();
                    assertThat(violation.getMessageKey()).isEqualTo("loan.renewal.active-penalty");
                    assertThat(violation.getMessageArgs()).containsExactly(penaltyExpiry.toLocalDate());
                });
    }

    @Test
    void validateRenewalRejectsUsedLimit() {
        assertThatThrownBy(() -> LoanPolicy.validateRenewal(LoanPolicy.MAX_RENEWALS, false, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessage("loan.renewal.limit-reached");
    }

    @Test
    void validateRenewalRejectsQueuedReservationFromOtherReader() {
        assertThatThrownBy(() -> LoanPolicy.validateRenewal(0, true, null))
                .isInstanceOf(LoanPolicyViolationException.class)
                .hasMessage("loan.renewal.queued-reservation");
    }
}
