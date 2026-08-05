package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.lumilivre.api.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "AppUser")
@Table(name = "app_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @Column(name = "email", nullable = false, length = 255, unique = true, columnDefinition = "citext")
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    @OneToOne
    @JoinColumn(name = "reader_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Reader reader;

    @Column(name = "preferred_locale", nullable = false, length = 10)
    @Builder.Default
    private String preferredLocale = "pt-BR";

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "guided_tour_completed", nullable = false)
    @Builder.Default
    private Boolean guidedTourCompleted = false;

    /** Desligamento administrativo: mantém o histórico e tira o acesso. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Bloqueio por segurança (suspeita de conta comprometida). */
    @Column(name = "locked", nullable = false)
    @Builder.Default
    private Boolean locked = false;

    /**
     * Geração de token válida para esta conta. O JWT carrega a versão vigente na
     * emissão; o filtro exige igualdade exata, então incrementar aqui mata na
     * hora todo token já emitido — sem guardar estado de token no servidor.
     */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (mustChangePassword == null) mustChangePassword = false;
        if (guidedTourCompleted == null) guidedTourCompleted = false;
        if (active == null) active = true;
        if (locked == null) locked = false;
        if (tokenVersion == null) tokenVersion = 0;
    }

    /**
     * Invalida todo token já emitido para esta conta. Incremento simples: não
     * importa quanto vale, importa que nenhum token antigo bate com o novo valor.
     */
    public void revokeIssuedTokens() {
        this.tokenVersion = (tokenVersion == null ? 0 : tokenVersion) + 1;
    }

    /**
     * Conta em condições de autenticar. Falha fechado de propósito: flag nula
     * (linha antiga, objeto montado à mão) não vale como permissão.
     */
    public boolean canAuthenticate() {
        return Boolean.TRUE.equals(active)
                && !Boolean.TRUE.equals(locked)
                && deletedAt == null;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
