package br.com.lumilivre.api.enums;

public enum ReservationStatus {

    WAITING("AGUARDANDO"),
    READY("DISPONIVEL_PARA_RETIRADA"),
    FULFILLED("CONVERTIDA"),
    CANCELLED("CANCELADA"),
    EXPIRED("EXPIRADA");

    private final String ptBrCode;

    ReservationStatus(String ptBrCode) {
        this.ptBrCode = ptBrCode;
    }

    public String getPtBrCode() {
        return ptBrCode;
    }

    public static ReservationStatus fromPtBrCode(String code) {
        for (ReservationStatus s : values()) {
            if (s.ptBrCode.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown ReservationStatus: " + code);
    }
}
