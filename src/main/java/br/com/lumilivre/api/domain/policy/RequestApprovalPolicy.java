package br.com.lumilivre.api.domain.policy;

import java.time.LocalDateTime;

import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.StatusSolicitacao;

/**
 * Regras de aprovação/rejeição de solicitações de empréstimo.
 */
public final class RequestApprovalPolicy {

    private RequestApprovalPolicy() {}

    /**
     * Valida se é possível criar uma solicitação de empréstimo.
     *
     * @param penaltyExpiry    expiração da penalidade do aluno (null = sem penalidade)
     * @param activeLoans      total de empréstimos ATIVO + ATRASADO do aluno
     * @param exemplarStatus   status atual do exemplar solicitado
     */
    public static void validateRequest(
            LocalDateTime penaltyExpiry,
            long activeLoans,
            StatusLivro exemplarStatus) {

        if (penaltyExpiry != null && penaltyExpiry.isAfter(LocalDateTime.now())) {
            throw new RequestApprovalViolationException("Aluno possui penalidade ativa.");
        }

        if (activeLoans >= LoanPolicy.MAX_ACTIVE_LOANS) {
            throw new RequestApprovalViolationException("Aluno atingiu limite de empréstimos ativos.");
        }

        BookAvailabilityPolicy.validateAvailable(exemplarStatus);
    }

    /**
     * Valida se uma solicitação pode ser processada (aceita ou rejeitada).
     * Uma solicitação já processada não pode ser alterada.
     */
    public static void validateProcessable(StatusSolicitacao currentStatus) {
        if (currentStatus != StatusSolicitacao.PENDENTE) {
            throw new RequestApprovalViolationException(
                    "Solicitação não está pendente. Status atual: " + currentStatus);
        }
    }

    public static class RequestApprovalViolationException extends RuntimeException {
        public RequestApprovalViolationException(String message) {
            super(message);
        }
    }
}
