package br.com.lumilivre.api.enums;

public enum AgeRating implements EnumStatus {

    CHILDREN("INFANTIL", "Infantil"),
    MIDDLE_GRADE("INFANTO_JUVENIL", "Infanto Juvenil"),
    TEEN("JUVENIL", "Juvenil"),
    ADULT("ADULTO", "Adulto"),
    GENERAL("LIVRE", "Livre");

    private final String ptBrCode;
    private final String label;

    AgeRating(String ptBrCode, String label) {
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

    public static AgeRating fromPtBrCode(String code) {
        for (AgeRating r : values()) {
            if (r.ptBrCode.equalsIgnoreCase(code) || r.name().equalsIgnoreCase(code)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown AgeRating: " + code);
    }
}
