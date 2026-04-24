package br.com.lumilivre.api.enums;

public enum ClassificacaoEtaria implements EnumStatus {

    CHILDREN("INFANTIL", "Infantil"),
    MIDDLE_GRADE("INFANTO_JUVENIL", "Infanto Juvenil"),
    TEEN("JUVENIL", "Juvenil"),
    ADULT("ADULTO", "Adulto"),
    GENERAL("LIVRE", "Livre");

    private final String ptBrCode;
    private final String label;

    ClassificacaoEtaria(String ptBrCode, String label) {
        this.ptBrCode = ptBrCode;
        this.label = label;
    }

    @Override
    public String getStatus() {
        return label;
    }

    @Override
    public String getPtBrCode() {
        return ptBrCode;
    }

    public static ClassificacaoEtaria fromPtBrCode(String code) {
        for (ClassificacaoEtaria c : values()) {
            if (c.ptBrCode.equalsIgnoreCase(code) || c.name().equalsIgnoreCase(code)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown ClassificacaoEtaria: " + code);
    }
}
