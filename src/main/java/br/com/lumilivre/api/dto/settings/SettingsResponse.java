package br.com.lumilivre.api.dto.settings;

import br.com.lumilivre.api.enums.LibraryType;

public record SettingsResponse(
        LibraryType libraryType,
        boolean readerCanEditAvatar,
        boolean guestAccessEnabled,
        SettingsFeaturesResponse features) {
}
