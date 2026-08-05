package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;

/**
 * As flags do {@link org.springframework.security.core.userdetails.UserDetails}
 * eram {@code true} fixo — ou seja, decorativas. Aqui elas viram contrato.
 */
class CustomUserDetailsTest {

    @Test
    void activeAccountWithoutDeletionIsEnabledAndUnlocked() {
        CustomUserDetails details = new CustomUserDetails(appUser(true, false, null));

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void inactiveAccountIsNotEnabled() {
        assertThat(new CustomUserDetails(appUser(false, false, null)).isEnabled()).isFalse();
    }

    @Test
    void softDeletedAccountIsNotEnabled() {
        assertThat(new CustomUserDetails(appUser(true, false, OffsetDateTime.now())).isEnabled()).isFalse();
    }

    @Test
    void lockedAccountIsNotAccountNonLocked() {
        assertThat(new CustomUserDetails(appUser(true, true, null)).isAccountNonLocked()).isFalse();
    }

    @Test
    void nullFlagsFromLegacyRowsDoNotGrantAccess() {
        AppUser appUser = appUser(true, false, null);
        appUser.setActive(null);

        assertThat(new CustomUserDetails(appUser).isEnabled()).isFalse();
    }

    @Test
    void authorityCarriesTheRolePrefix() {
        CustomUserDetails details = new CustomUserDetails(appUser(true, false, null));

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_LIBRARIAN");
    }

    private static AppUser appUser(boolean active, boolean locked, OffsetDateTime deletedAt) {
        return AppUser.builder()
                .email("librarian@lumilivre.test")
                .passwordHash("hash")
                .role(Role.LIBRARIAN)
                .active(active)
                .locked(locked)
                .deletedAt(deletedAt)
                .build();
    }
}
