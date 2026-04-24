package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy.RequestApprovalViolationException;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.StatusSolicitacao;

class RequestApprovalPolicyTest {

    @Test
    void validateRequestAllowsValidRequest() {
        assertThatCode(() -> RequestApprovalPolicy.validateRequest(
                null,
                LoanPolicy.MAX_ACTIVE_LOANS - 1,
                StatusLivro.AVAILABLE))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRequestAllowsExpiredPenalty() {
        assertThatCode(() -> RequestApprovalPolicy.validateRequest(
                LocalDateTime.now().minusMinutes(1),
                0,
                StatusLivro.AVAILABLE))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRequestRejectsActivePenalty() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateRequest(
                LocalDateTime.now().plusMinutes(1),
                0,
                StatusLivro.AVAILABLE))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessageContaining("penalidade");
    }

    @Test
    void validateRequestRejectsLoanLimitBoundary() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateRequest(
                null,
                LoanPolicy.MAX_ACTIVE_LOANS,
                StatusLivro.AVAILABLE))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessageContaining("limite");
    }

    @Test
    void validateRequestRejectsUnavailableBook() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateRequest(
                null,
                0,
                StatusLivro.BORROWED))
                .isInstanceOf(BookAvailabilityViolationException.class);
    }

    @Test
    void validateProcessableAllowsPendingRequest() {
        assertThatCode(() -> RequestApprovalPolicy.validateProcessable(StatusSolicitacao.PENDING))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = StatusSolicitacao.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void validateProcessableRejectsAlreadyProcessedRequests(StatusSolicitacao status) {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateProcessable(status))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessageContaining("pendente");
    }

    @Test
    void validateProcessableRejectsNullStatus() {
        assertThatThrownBy(() -> RequestApprovalPolicy.validateProcessable(null))
                .isInstanceOf(RequestApprovalViolationException.class);
    }
}
