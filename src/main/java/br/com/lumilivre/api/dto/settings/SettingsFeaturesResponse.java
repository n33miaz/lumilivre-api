package br.com.lumilivre.api.dto.settings;

public record SettingsFeaturesResponse(
        boolean academicFields,
        boolean ranking,
        boolean contents) {
}
