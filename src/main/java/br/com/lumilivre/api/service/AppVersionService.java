package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.appversion.AppVersionRequest;
import br.com.lumilivre.api.dto.appversion.AppVersionResponse;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppVersion;
import br.com.lumilivre.api.repository.AppVersionRepository;
import br.com.lumilivre.api.security.Auditable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppVersionService {

    private static final Set<String> PLATFORMS = Set.of("ANDROID", "IOS");

    private final AppVersionRepository repository;

    @Transactional(readOnly = true)
    public AppVersionResponse get(String platform) {
        String normalized = normalize(platform);
        AppVersion version = repository.findById(normalized)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("app-version.not-found"));
        // O GET é público (o app consulta antes do login). Só expõe
        // updatedBy/updatedAt a chamadores autenticados (painel admin) — para o
        // app anônimo esses campos vêm nulos e não vazam o username do admin.
        return toResponse(version, isAuthenticatedStaff());
    }

    @Transactional
    @Auditable(action = "APP_VERSION_UPDATED", targetParam = "#request.platform")
    public AppVersionResponse update(AppVersionRequest request, String updatedBy) {
        String normalized = normalize(request.platform());

        AppVersion version = repository.findById(normalized).orElseGet(() -> {
            AppVersion created = new AppVersion();
            created.setPlatform(normalized);
            return created;
        });

        version.setLatestVersion(request.latestVersion());
        version.setLatestBuild(request.latestBuild());
        version.setMinSupportedVersion(request.minSupportedVersion());
        version.setMinSupportedBuild(request.minSupportedBuild());
        version.setForceUpdate(Boolean.TRUE.equals(request.forceUpdate()));
        version.setUpdateMessage(request.updateMessage());
        version.setStoreUrlAndroid(request.storeUrlAndroid());
        version.setStoreUrlIos(request.storeUrlIos());
        version.setUpdatedBy(updatedBy);
        if (version.getUpdatedAt() == null) {
            version.setUpdatedAt(OffsetDateTime.now());
        }

        return toResponse(repository.save(version), true);
    }

    private boolean isAuthenticatedStaff() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getPrincipal() instanceof br.com.lumilivre.api.security.CustomUserDetails;
    }

    private String normalize(String platform) {
        if (platform == null) {
            throw BusinessRuleException.ofKey("app-version.platform.invalid");
        }
        String normalized = platform.trim().toUpperCase(Locale.ROOT);
        if (!PLATFORMS.contains(normalized)) {
            throw BusinessRuleException.ofKey("app-version.platform.invalid");
        }
        return normalized;
    }

    private AppVersionResponse toResponse(AppVersion v, boolean includeAudit) {
        String storeUrl = "IOS".equals(v.getPlatform()) ? v.getStoreUrlIos() : v.getStoreUrlAndroid();
        return new AppVersionResponse(
                v.getPlatform(),
                v.getLatestVersion(),
                v.getLatestBuild(),
                v.getMinSupportedVersion(),
                v.getMinSupportedBuild(),
                Boolean.TRUE.equals(v.getForceUpdate()),
                v.getUpdateMessage(),
                storeUrl,
                includeAudit ? v.getUpdatedAt() : null,
                includeAudit ? v.getUpdatedBy() : null);
    }
}
