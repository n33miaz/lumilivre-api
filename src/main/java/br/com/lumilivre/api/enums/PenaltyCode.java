package br.com.lumilivre.api.enums;

public enum PenaltyCode implements EnumStatus {

    RECORD("REGISTRO", "Registro", 1),
    WARNING("ADVERTENCIA", "Advertência", 2),
    SUSPENSION("SUSPENSAO", "Suspensão", 3),
    BLOCK("BLOQUEIO", "Bloqueio", 4),
    BAN("BANIMENTO", "Banimento", 5);

    private final String ptBrCode;
    private final String label;
    private final int severity;

    PenaltyCode(String ptBrCode, String label, int severity) {
        this.ptBrCode = ptBrCode;
        this.label = label;
        this.severity = severity;
    }

    @Override
    public String getStatus() {
        return label;
    }

    @Override
    public String getPtBrCode() {
        return ptBrCode;
    }

    public int getSeverity() {
        return severity;
    }

    public static PenaltyCode fromPtBrCode(String code) {
        for (PenaltyCode p : values()) {
            if (p.ptBrCode.equalsIgnoreCase(code) || p.name().equalsIgnoreCase(code)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown PenaltyCode: " + code);
    }

    public static PenaltyCode fromDaysLate(long daysLate) {
        if (daysLate <= 1)  return RECORD;
        if (daysLate <= 5)  return WARNING;
        if (daysLate <= 7)  return SUSPENSION;
        if (daysLate <= 90) return BLOCK;
        return BAN;
    }

    public boolean isMoreSevereThan(PenaltyCode other) {
        if (other == null) return true;
        return this.severity > other.severity;
    }
}
