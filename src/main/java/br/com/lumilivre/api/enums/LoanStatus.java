package br.com.lumilivre.api.enums;

public enum LoanStatus implements EnumStatus {

    ACTIVE("ATIVO", "Ativo"),
    COMPLETED("CONCLUIDO", "Concluído"),
    OVERDUE("ATRASADO", "Atrasado");

    private final String ptBrCode;
    private final String label;

    LoanStatus(String ptBrCode, String label) {
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

    public static LoanStatus fromPtBrCode(String code) {
        for (LoanStatus s : values()) {
            if (s.ptBrCode.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown LoanStatus: " + code);
    }
}
