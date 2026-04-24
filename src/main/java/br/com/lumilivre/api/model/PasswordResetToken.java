package br.com.lumilivre.api.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity(name = "PasswordResetToken")
@Table(name = "token_reset_senha")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(targetEntity = AppUser.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "usuario_id")
    @ToString.Exclude
    private AppUser appUser;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDateTime expiresAt;

    public PasswordResetToken(String token, AppUser appUser, int minutosParaExpirar) {
        this.token = token;
        this.appUser = appUser;
        this.expiresAt = LocalDateTime.now().plusMinutes(minutosParaExpirar);
    }

    public AppUser getUsuario() {
        return appUser;
    }

    public void setUsuario(AppUser usuario) {
        this.appUser = usuario;
    }

    public LocalDateTime getDataExpiracao() {
        return expiresAt;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.expiresAt = dataExpiracao;
    }

    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
