package br.com.lumilivre.api.dto.settings;

import br.com.lumilivre.api.enums.LibraryType;
import jakarta.validation.constraints.NotNull;

public record SettingsRequest(
        @NotNull LibraryType libraryType,
        // Opcional: quando null, mantém o valor atual (compat com clients antigos).
        Boolean readerCanEditAvatar) {
}
