package br.com.lumilivre.api.dto.appversion;

import java.time.OffsetDateTime;

/** Resposta pública de {@code appVersion.get} (o app consome na inicialização). */
public record AppVersionResponse(
        String platform,
        String latestVersion,
        Integer latestBuild,
        String minSupportedVersion,
        Integer minSupportedBuild,
        boolean forceUpdate,
        String updateMessage,
        String storeUrl,
        OffsetDateTime updatedAt,
        String updatedBy) {
}
