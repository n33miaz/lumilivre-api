package br.com.lumilivre.api.dto.audit;

import java.time.OffsetDateTime;

/** Item da trilha de acessos para o viewer admin. */
public record AccessLogResponse(
        Long id,
        String actor,
        String actorRole,
        String event,
        String channel,
        String result,
        /** Recurso consultado nos eventos de uso; nulo nos de autenticação. */
        String targetId,
        String ipAddress,
        String userAgent,
        String correlationId,
        String errorMessage,
        OffsetDateTime occurredAt) {
}
