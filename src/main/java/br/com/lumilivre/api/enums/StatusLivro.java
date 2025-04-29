package br.com.lumilivre.api.enums;

public enum StatusLivro implements EnumStatus{
    DISPONIVEL("Disponível"),
    INDISPONIVEL("Indisponível"),
    EM_MANUTENCAO("Em manutenção");

    private final String status;

    StatusLivro(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
