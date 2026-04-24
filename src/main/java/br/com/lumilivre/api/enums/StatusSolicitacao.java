package br.com.lumilivre.api.enums;

public enum StatusSolicitacao {

    PENDING("PENDENTE"),
    ACCEPTED("ACEITA"),
    REJECTED("REJEITADA"),
    CANCELLED("CANCELADA");

    private final String ptBrCode;

    StatusSolicitacao(String ptBrCode) {
        this.ptBrCode = ptBrCode;
    }

    public String getPtBrCode() {
        return ptBrCode;
    }

    public static StatusSolicitacao fromPtBrCode(String code) {
        for (StatusSolicitacao s : values()) {
            if (s.ptBrCode.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown StatusSolicitacao: " + code);
    }
}
