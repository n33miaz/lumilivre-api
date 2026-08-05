package br.com.lumilivre.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.settings.SettingsFeaturesResponse;
import br.com.lumilivre.api.dto.settings.SettingsPublicResponse;
import br.com.lumilivre.api.dto.settings.SettingsRequest;
import br.com.lumilivre.api.dto.settings.SettingsResponse;
import br.com.lumilivre.api.enums.LibraryType;
import br.com.lumilivre.api.model.LibrarySettings;
import br.com.lumilivre.api.repository.LibrarySettingsRepository;
import br.com.lumilivre.api.security.Auditable;

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

    /** Permissão global de troca de foto pelo app. Default TRUE. */
    @Transactional(readOnly = true)
    public boolean isReaderCanEditAvatar() {
        return Boolean.TRUE.equals(getOrCreateSettings().getReaderCanEditAvatar());
    }

    // Não existe aqui um `isGuestAccessEnabled()` para uso interno, ao contrário
    // de isReaderCanEditAvatar(): `guest_access_enabled` é, hoje, um toggle de
    // produto que o cliente honra, e não uma barreira de autorização. Aplicá-lo
    // no servidor significaria recusar /api/books/catalog, /public/search e
    // /genres/** a chamador anônimo — e esses endpoints não servem só o modo
    // convidado do app: são a vitrine pública do acervo. Desligar a flag
    // derrubaria também isso. Um acessor sem uso sugeriria uma proteção que não
    // existe, o que é pior que a ausência dela; a decisão está registrada no
    // relatório do T04 para o dono resolver o escopo.

    @Transactional
    public SettingsResponse getSettingsView() {
        return toResponse(getOrCreateSettings());
    }

    /**
     * Recorte anônimo: sem ele o convidado nunca descobre que o acesso de
     * convidado foi desligado — pediria o catálogo, tomaria erro e leria isso
     * como falha de rede, que era exatamente o sintoma relatado no app.
     */
    @Transactional
    public SettingsPublicResponse getPublicSettingsView() {
        LibrarySettings settings = getOrCreateSettings();
        return new SettingsPublicResponse(
                settings.getLibraryType(),
                Boolean.TRUE.equals(settings.getGuestAccessEnabled()),
                features(settings));
    }

    @Transactional
    @Auditable(action = "SETTINGS_UPDATED")
    public SettingsResponse update(SettingsRequest request) {
        LibrarySettings settings = getOrCreateSettings();
        settings.setLibraryType(request.libraryType());
        // Campo opcional: só altera quando enviado (mantém compat com clients antigos).
        if (request.readerCanEditAvatar() != null) {
            settings.setReaderCanEditAvatar(request.readerCanEditAvatar());
        }
        if (request.guestAccessEnabled() != null) {
            settings.setGuestAccessEnabled(request.guestAccessEnabled());
        }
        return toResponse(repository.save(settings));
    }

    private LibrarySettings getOrCreateSettings() {
        return repository.findById(SINGLETON_ID)
                .orElseGet(() -> repository.save(LibrarySettings.builder()
                        .id(SINGLETON_ID)
                        .libraryType(LibraryType.SCHOOL)
                        .readerCanEditAvatar(Boolean.TRUE)
                        .guestAccessEnabled(Boolean.TRUE)
                        .build()));
    }

    private SettingsResponse toResponse(LibrarySettings settings) {
        return new SettingsResponse(
                settings.getLibraryType(),
                Boolean.TRUE.equals(settings.getReaderCanEditAvatar()),
                Boolean.TRUE.equals(settings.getGuestAccessEnabled()),
                features(settings));
    }

    private SettingsFeaturesResponse features(LibrarySettings settings) {
        boolean school = settings.getLibraryType() == LibraryType.SCHOOL;
        // `contents` (ex-`thesis`): comunicados fazem sentido em qualquer biblioteca,
        // entao a feature fica sempre habilitada; apenas o tipo WORK/TCC e destacado
        // quando SCHOOL. Campos academicos e ranking seguem o tipo de biblioteca.
        return new SettingsFeaturesResponse(school, school, true);
    }
}
