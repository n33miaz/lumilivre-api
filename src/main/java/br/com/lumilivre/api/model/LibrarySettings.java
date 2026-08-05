package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;

import br.com.lumilivre.api.enums.LibraryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibrarySettings {

    @Id
    @Column(name = "id", nullable = false)
    private Boolean id;

    @Enumerated(EnumType.STRING)
    @Column(name = "library_type", nullable = false, length = 20)
    private LibraryType libraryType;

    @Column(name = "reader_can_edit_avatar", nullable = false)
    private Boolean readerCanEditAvatar;

    /** Permite navegar o acervo sem login (modo convidado do app). Default TRUE. */
    @Column(name = "guest_access_enabled", nullable = false)
    private Boolean guestAccessEnabled;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = Boolean.TRUE;
        if (libraryType == null) libraryType = LibraryType.SCHOOL;
        if (readerCanEditAvatar == null) readerCanEditAvatar = Boolean.TRUE;
        if (guestAccessEnabled == null) guestAccessEnabled = Boolean.TRUE;
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
