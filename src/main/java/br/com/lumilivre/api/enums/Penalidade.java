package br.com.lumilivre.api.enums;

public enum Penalidade implements EnumStatus {

    RECORD("REGISTRO", "Registro", 1),
    WARNING("ADVERTENCIA", "Advertência", 2),
    SUSPENSION("SUSPENSAO", "Suspensão", 3),
    BLOCK("BLOQUEIO", "Bloqueio", 4),
    BAN("BANIMENTO", "Banimento", 5);

    private final String ptBrCode;
    private final String label;
    private final int severity;

    Penalidade(String ptBrCode, String label, int severity) {
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

    public int getGravidade() {
        return severity;
    }

    public static Penalidade fromPtBrCode(String code) {
        for (Penalidade p : values()) {
            if (p.ptBrCode.equalsIgnoreCase(code) || p.name().equalsIgnoreCase(code)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown Penalidade: " + code);
    }

    public static Penalidade fromDiasDeAtraso(long diasDeAtraso) {
        if (diasDeAtraso <= 1) return RECORD;
        if (diasDeAtraso <= 5) return WARNING;
        if (diasDeAtraso <= 7) return SUSPENSION;
        if (diasDeAtraso <= 90) return BLOCK;
        return BAN;
    }

    public boolean isMaisGraveQue(Penalidade outra) {
        if (outra == null) return true;
        return this.severity > outra.severity;
    }
}
