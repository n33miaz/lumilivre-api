package br.com.lumilivre.api.enums;

public enum StatusLivro implements EnumStatus {

    AVAILABLE("DISPONIVEL", "Disponível"),
    UNAVAILABLE("INDISPONIVEL", "Indisponível"),
    MAINTENANCE("EM_MANUTENCAO", "Em manutenção"),
    BORROWED("EMPRESTADO", "Emprestado");

    private final String ptBrCode;
    private final String label;

    StatusLivro(String ptBrCode, String label) {
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

    public static StatusLivro fromPtBrCode(String code) {
        for (StatusLivro s : values()) {
            if (s.ptBrCode.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown StatusLivro: " + code);
    }
}
