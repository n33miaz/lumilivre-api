package br.com.lumilivre.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.settings.SettingsFeaturesResponse;
import br.com.lumilivre.api.dto.settings.SettingsRequest;
import br.com.lumilivre.api.dto.settings.SettingsResponse;
import br.com.lumilivre.api.enums.LibraryType;
import br.com.lumilivre.api.model.LibrarySettings;
import br.com.lumilivre.api.repository.LibrarySettingsRepository;

@Service
public class SettingsService {

    private static final Boolean SINGLETON_ID = Boolean.TRUE;

    private final LibrarySettingsRepository repository;

    public SettingsService(LibrarySettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LibraryType getLibraryType() {
        return getOrCreateSettings().getLibraryType();
    }

    /** Permissão global de troca de foto pelo app (WS-02). Default TRUE. */
    @Transactional(readOnly = true)
    public boolean isReaderCanEditAvatar() {
        return Boolean.TRUE.equals(getOrCreateSettings().getReaderCanEditAvatar());
    }

    @Transactional
    public SettingsResponse getSettingsView() {
        return toResponse(getOrCreateSettings());
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        LibrarySettings settings = getOrCreateSettings();
        settings.setLibraryType(request.libraryType());
        // Campo opcional: só altera quando enviado (mantém compat com clients antigos).
        if (request.readerCanEditAvatar() != null) {
            settings.setReaderCanEditAvatar(request.readerCanEditAvatar());
        }
        return toResponse(repository.save(settings));
    }

    private LibrarySettings getOrCreateSettings() {
        return repository.findById(SINGLETON_ID)
                .orElseGet(() -> repository.save(LibrarySettings.builder()
                        .id(SINGLETON_ID)
                        .libraryType(LibraryType.SCHOOL)
                        .readerCanEditAvatar(Boolean.TRUE)
                        .build()));
    }

    private SettingsResponse toResponse(LibrarySettings settings) {
        boolean school = settings.getLibraryType() == LibraryType.SCHOOL;
        // `contents` (ex-`thesis`): comunicados fazem sentido em qualquer biblioteca,
        // entao a feature fica sempre habilitada; apenas o tipo WORK/TCC e destacado
        // quando SCHOOL. Campos academicos e ranking seguem o tipo de biblioteca.
        return new SettingsResponse(
                settings.getLibraryType(),
                Boolean.TRUE.equals(settings.getReaderCanEditAvatar()),
                new SettingsFeaturesResponse(school, school, true));
    }
}
