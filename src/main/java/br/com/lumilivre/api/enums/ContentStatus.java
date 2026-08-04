package br.com.lumilivre.api.enums;

import java.time.OffsetDateTime;

/**
 * Estado derivado (nao persistido) de {@link br.com.lumilivre.api.model.AppContent},
 * calculado a partir de {@code is_published} e da janela de publicacao. Usado
 * apenas na resposta do painel admin para o badge de status.
 */
public enum ContentStatus {
    /** Publicado e dentro da janela (visivel agora). */
    PUBLISHED,
    /** Publicado, mas a janela ainda nao comecou. */
    SCHEDULED,
    /** Publicado, mas a janela ja terminou. */
    EXPIRED,
    /** Despublicado (toggle desligado). */
    HIDDEN;

    /**
     * Deriva o status considerando o instante atual.
     */
    public static ContentStatus resolve(boolean published, OffsetDateTime start, OffsetDateTime end, OffsetDateTime now) {
        if (!published) {
            return HIDDEN;
        }
        if (start != null && start.isAfter(now)) {
            return SCHEDULED;
        }
        if (end != null && end.isBefore(now)) {
            return EXPIRED;
        }
        return PUBLISHED;
    }
}
