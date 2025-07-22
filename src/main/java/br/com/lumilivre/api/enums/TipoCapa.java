package br.com.lumilivre.api.enums;

public enum TipoCapa implements EnumStatus {
    DURA("Capa dura"),
    FLEXIVEL("Capa Flexivel"),
    CARTONADA("Capa Cartonada"),
    CAPA_COM_ORELHAS("Capa com Orelhas"),
    ESPIRAL("Capa Espiral");

    private final String status;

    TipoCapa(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}
