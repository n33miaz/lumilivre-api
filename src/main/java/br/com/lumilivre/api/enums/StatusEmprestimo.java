package br.com.lumilivre.api.enums;

public enum StatusEmprestimo implements EnumStatus {

    ATIVO("Ativo"),
    CONCLUIDO("Concluído"),
    ATRASADO("Atrasado");

    private final String status;

    StatusEmprestimo(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
