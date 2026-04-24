package br.com.lumilivre.api.enums;

public enum StatusReserva {

    WAITING("AGUARDANDO"),
    READY("DISPONIVEL_PARA_RETIRADA"),
    FULFILLED("CONVERTIDA"),
    CANCELLED("CANCELADA"),
    EXPIRED("EXPIRADA");

    private final String ptBrCode;

    StatusReserva(String ptBrCode) {
        this.ptBrCode = ptBrCode;
    }

    public String getPtBrCode() {
        return ptBrCode;
    }

    public static StatusReserva fromPtBrCode(String code) {
        for (StatusReserva s : values()) {
            if (s.ptBrCode.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown StatusReserva: " + code);
    }
}
