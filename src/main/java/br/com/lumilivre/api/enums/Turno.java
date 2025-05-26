package br.com.lumilivre.api.enums;

public enum Turno {

    MANHA("Manhã"),
    TARDE("Tarde"),
    NOITE("Noite"),
    INTGERAL("Integral");

    private final String status;

    Turno(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
