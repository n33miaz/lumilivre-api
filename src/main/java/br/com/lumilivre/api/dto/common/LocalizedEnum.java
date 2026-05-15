package br.com.lumilivre.api.dto.common;

public record LocalizedEnum(String code, String label) {

    public static LocalizedEnum of(Enum<?> e, String label) {
        return new LocalizedEnum(e.name(), label);
    }
}
