package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Controle de versão do app por plataforma (ANDROID/IOS). Ver V3.
 */
@Entity
@Table(name = "app_version")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppVersion {

    @Id
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "latest_version", nullable = false, length = 20)
    private String latestVersion;

    @Column(name = "latest_build", nullable = false)
    private Integer latestBuild;

    @Column(name = "min_supported_version", nullable = false, length = 20)
    private String minSupportedVersion;

    @Column(name = "min_supported_build", nullable = false)
    private Integer minSupportedBuild;

    @Builder.Default
    @Column(name = "force_update", nullable = false)
    private Boolean forceUpdate = false;

    @Column(name = "update_message", columnDefinition = "text")
    private String updateMessage;

    @Column(name = "store_url_android", length = 512)
    private String storeUrlAndroid;

    @Column(name = "store_url_ios", length = 512)
    private String storeUrlIos;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
