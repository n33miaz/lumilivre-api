package br.com.lumilivre.api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Turno {

    MANHA("Manhã"),
    TARDE("Tarde"),
    NOITE("Noite"),
    INTEGRAL("Integral");

    @JsonCreator
    public static Turno fromString(String value) {
        for (Turno turno : values()) {
            if (turno.name().equalsIgnoreCase(value)) {
                return turno;
            }
        }
        throw new IllegalArgumentException("Turno inválido: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }

    private final String status;

    Turno(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
