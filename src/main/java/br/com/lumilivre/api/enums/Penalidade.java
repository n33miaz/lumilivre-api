package br.com.lumilivre.api.enums;

public enum Penalidade {

    REGISTRO("Registro"),
    ADVERTENCIA("Advertência"),
    SUSPENSAO("Suspensão"),
    BLOQUEIO("Bloqueio"),
    BANIMENTO("Banimento");

    private final String status;

    Penalidade(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
