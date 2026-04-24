package br.com.lumilivre.api.enums;

public enum TipoCapa implements EnumStatus {

    HARDCOVER("DURA", "Capa dura"),
    SOFTCOVER("FLEXIVEL", "Capa Flexível"),
    BOARD_BOOK("CARTONADA", "Capa Cartonada"),
    DUST_JACKET("CAPA_COM_ORELHAS", "Capa com Orelhas"),
    SPIRAL("ESPIRAL", "Capa Espiral"),
    PAPERBACK("BROCHURA", "Brochura");

    private final String ptBrCode;
    private final String label;

    TipoCapa(String ptBrCode, String label) {
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

    public static TipoCapa fromPtBrCode(String code) {
        for (TipoCapa t : values()) {
            if (t.ptBrCode.equalsIgnoreCase(code) || t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown TipoCapa: " + code);
    }
}
