package br.com.lumilivre.api.enums;

public enum StatusEmprestimo implements EnumStatus {

    ACTIVE("ATIVO", "Ativo"),
    COMPLETED("CONCLUIDO", "Concluído"),
    OVERDUE("ATRASADO", "Atrasado");

    private final String ptBrCode;
    private final String label;

    StatusEmprestimo(String ptBrCode, String label) {
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

    public static StatusEmprestimo fromPtBrCode(String code) {
        for (StatusEmprestimo s : values()) {
            if (s.ptBrCode.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown StatusEmprestimo: " + code);
    }
}
