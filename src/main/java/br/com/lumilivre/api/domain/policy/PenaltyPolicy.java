package br.com.lumilivre.api.domain.policy;

import br.com.lumilivre.api.enums.Penalidade;

/**
 * Regras de cálculo de penalidade por atraso na devolução.
 * Intervalos conforme especificação:
 *   0–1 dias  → REGISTRO
 *   2–5 dias  → ADVERTENCIA
 *   6–7 dias  → SUSPENSAO
 *   8–90 dias → BLOQUEIO
 *   > 90 dias → BANIMENTO
 */
public final class PenaltyPolicy {

    private PenaltyPolicy() {}

    public static Penalidade calculate(long daysLate) {
        if (daysLate < 0) {
            throw new IllegalArgumentException("Dias de atraso não pode ser negativo: " + daysLate);
        }
        if (daysLate <= 1)  return Penalidade.RECORD;
        if (daysLate <= 5)  return Penalidade.WARNING;
        if (daysLate <= 7)  return Penalidade.SUSPENSION;
        if (daysLate <= 90) return Penalidade.BLOCK;
        return Penalidade.BAN;
    }

    /** Retorna true se {@code candidate} é mais grave que {@code current} (ou current é null). */
    public static boolean isMoreSevere(Penalidade candidate, Penalidade current) {
        if (current == null) return true;
        return candidate.getGravidade() > current.getGravidade();
    }
}
