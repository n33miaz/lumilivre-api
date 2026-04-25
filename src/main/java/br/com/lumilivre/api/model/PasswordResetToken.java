package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;

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
@Table(name = "password_reset_token")
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
    @JoinColumn(nullable = false, name = "app_user_id")
    @ToString.Exclude
    private AppUser appUser;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PasswordResetToken(String token, AppUser appUser, int minutesToExpire) {
        this.token = token;
        this.appUser = appUser;
        OffsetDateTime now = OffsetDateTime.now();
        this.expiresAt = now.plusMinutes(minutesToExpire);
        this.createdAt = now;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.expiresAt);
    }
}
