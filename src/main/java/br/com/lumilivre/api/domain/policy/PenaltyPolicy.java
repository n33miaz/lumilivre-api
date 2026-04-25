package br.com.lumilivre.api.domain.policy;

import br.com.lumilivre.api.enums.PenaltyCode;

public final class PenaltyPolicy {

    private PenaltyPolicy() {}

    public static PenaltyCode calculate(long daysLate) {
        if (daysLate < 0) {
            throw new IllegalArgumentException("Days late cannot be negative: " + daysLate);
        }
        return PenaltyCode.fromDaysLate(daysLate);
    }

    public static boolean isMoreSevere(PenaltyCode candidate, PenaltyCode current) {
        if (current == null) return true;
        return candidate.getSeverity() > current.getSeverity();
    }
}
