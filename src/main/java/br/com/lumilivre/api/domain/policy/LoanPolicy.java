package br.com.lumilivre.api.domain.policy;

import java.time.LocalDateTime;

/**
 * Regras de negócio puras para empréstimos.
 * Sem dependência de Spring ou infraestrutura.
 */
public final class LoanPolicy {

    public static final int MAX_ACTIVE_LOANS = 3;

    private LoanPolicy() {}

    /**
     * Verifica se o aluno pode tomar um novo empréstimo.
     *
     * @param activeLoans   quantidade atual de empréstimos ATIVO + ATRASADO
     * @param penaltyExpiry data de expiração da penalidade ativa (null se sem penalidade)
     */
    public static void validateNewLoan(long activeLoans, LocalDateTime penaltyExpiry) {
        if (penaltyExpiry != null && penaltyExpiry.isAfter(LocalDateTime.now())) {
            throw new LoanPolicyViolationException(
                    "Aluno possui penalidade ativa até " + penaltyExpiry);
        }

        if (activeLoans >= MAX_ACTIVE_LOANS) {
            throw new LoanPolicyViolationException(
                    "Aluno já atingiu o limite de " + MAX_ACTIVE_LOANS + " empréstimos ativos.");
        }
    }

    /** Maximum number of renewals allowed per loan. */
    public static final int MAX_RENEWALS = 2;

    /** Extension in days granted per renewal. */
    public static final int RENEWAL_DAYS = 14;

    /**
     * Validates whether a loan can be renewed.
     *
     * @param currentRenewals renewals already used for this loan
     * @param hasQueuedReservation whether another student is waiting for this book
     * @param penaltyExpiry active penalty expiry (null = no penalty)
     */
    public static void validateRenewal(int currentRenewals, boolean hasQueuedReservation,
                                       LocalDateTime penaltyExpiry) {
        if (penaltyExpiry != null && penaltyExpiry.isAfter(LocalDateTime.now())) {
            throw new LoanPolicyViolationException(
                    "Aluno possui penalidade ativa. Renovação bloqueada.");
        }
        if (hasQueuedReservation) {
            throw new LoanPolicyViolationException(
                    "Não é possível renovar: outro aluno está aguardando este livro na fila de reservas.");
        }
        if (currentRenewals >= MAX_RENEWALS) {
            throw new LoanPolicyViolationException(
                    "Limite de " + MAX_RENEWALS + " renovação(ões) por empréstimo atingido.");
        }
    }

    public static class LoanPolicyViolationException extends RuntimeException {
        public LoanPolicyViolationException(String message) {
            super(message);
        }
    }
}
