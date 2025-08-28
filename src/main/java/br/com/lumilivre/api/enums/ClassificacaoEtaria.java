package br.com.lumilivre.api.enums;

public enum ClassificacaoEtaria implements EnumStatus {
    INFANTIL("Infantil"),
    INFANTO_JUVENIL("Infanto Juvenil"),
    JUVENIL("Juvenil"),
    ADULTO("Adulto"),
    LIVRE("Livre");

    private final String status;

    ClassificacaoEtaria(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
