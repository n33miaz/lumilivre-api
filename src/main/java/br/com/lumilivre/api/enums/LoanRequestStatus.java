package br.com.lumilivre.api.enums;

public enum LoanRequestStatus {

    PENDING("PENDENTE"),
    ACCEPTED("ACEITA"),
    REJECTED("REJEITADA"),
    CANCELLED("CANCELADA");

    private final String ptBrCode;

    LoanRequestStatus(String ptBrCode) {
        this.ptBrCode = ptBrCode;
    }

    public String getPtBrCode() {
        return ptBrCode;
    }

    public static LoanRequestStatus fromPtBrCode(String code) {
        for (LoanRequestStatus s : values()) {
            if (s.ptBrCode.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown LoanRequestStatus: " + code);
    }
}
