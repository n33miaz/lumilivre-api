package br.com.lumilivre.api.dto.settings;

import br.com.lumilivre.api.enums.LibraryType;

/**
 * Subconjunto de {@link SettingsResponse} entregue a chamador anônimo.
 *
 * <p>É um record próprio, e não o mesmo DTO com campos nulos, justamente para
 * que a decisão de expor volte a ser tomada campo por campo: {@code
 * library_settings} é a linha única de configuração e vai receber flags novas
 * (interesse, políticas de empréstimo, retenção). Se o endpoint público
 * devolvesse o objeto inteiro, a próxima flag vazaria sem ninguém decidir nada
 * — o vazamento seria consequência da forma do endpoint, não de uma escolha.
 *
 * <p>Aqui entra só o que o convidado precisa para desenhar a primeira tela:
 * se o modo convidado está ligado, o tipo de biblioteca e as features que
 * derivam dele (abas do app). {@code readerCanEditAvatar} fica fora — é
 * permissão de leitor autenticado e não tem uso antes do login.
 */
public record SettingsPublicResponse(
        LibraryType libraryType,
        boolean guestAccessEnabled,
        SettingsFeaturesResponse features) {
}
