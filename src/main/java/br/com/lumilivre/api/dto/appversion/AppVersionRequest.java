package br.com.lumilivre.api.dto.appversion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload de {@code appVersion.update} (admin). */
public record AppVersionRequest(
        @NotBlank String platform,
        @NotBlank String latestVersion,
        @NotNull @Min(0) Integer latestBuild,
        @NotBlank String minSupportedVersion,
        @NotNull @Min(0) Integer minSupportedBuild,
        Boolean forceUpdate,
        String updateMessage,
        String storeUrlAndroid,
        String storeUrlIos) {
}
