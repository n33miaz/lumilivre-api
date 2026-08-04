package br.com.lumilivre.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.settings.SettingsFeaturesResponse;
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

    @Transactional
    public SettingsResponse getSettingsView() {
        return toResponse(getOrCreateSettings().getLibraryType());
    }

    @Transactional
    public SettingsResponse updateLibraryType(LibraryType libraryType) {
        LibrarySettings settings = getOrCreateSettings();
        settings.setLibraryType(libraryType);
        return toResponse(repository.save(settings).getLibraryType());
    }

    private LibrarySettings getOrCreateSettings() {
        return repository.findById(SINGLETON_ID)
                .orElseGet(() -> repository.save(LibrarySettings.builder()
                        .id(SINGLETON_ID)
                        .libraryType(LibraryType.SCHOOL)
                        .build()));
    }

    private SettingsResponse toResponse(LibraryType libraryType) {
        boolean school = libraryType == LibraryType.SCHOOL;
        // `contents` (ex-`thesis`): comunicados fazem sentido em qualquer biblioteca,
        // entao a feature fica sempre habilitada; apenas o tipo WORK/TCC e destacado
        // quando SCHOOL. Campos academicos e ranking seguem o tipo de biblioteca.
        return new SettingsResponse(
                libraryType,
                new SettingsFeaturesResponse(school, school, true));
    }
}
