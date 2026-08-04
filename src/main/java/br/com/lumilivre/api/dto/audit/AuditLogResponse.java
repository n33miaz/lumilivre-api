package br.com.lumilivre.api.dto.audit;

import java.time.OffsetDateTime;

/** Item da auditoria de negócio para o viewer admin (WS-07/WS-09). */
public record AuditLogResponse(
        Long id,
        String actor,
        String actorRole,
        String targetId,
        String action,
        String result,
        String ipAddress,
        String errorMessage,
        OffsetDateTime occurredAt) {
}
