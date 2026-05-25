package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.auth.ChangePasswordRequest;
import br.com.lumilivre.api.dto.user.UserRequest;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.service.infra.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private MessageResolver messages;

    @Captor
    private ArgumentCaptor<AppUser> appUserCaptor;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAdminDefaultsRoleToLibrarianAndSendsInitialPasswordEmail() {
        UserRequest request = UserRequest.builder()
                .email("librarian@example.test")
                .password("initial-pass")
                .build();
        when(passwordEncoder.encode("initial-pass")).thenReturn("encoded-pass");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.resolve("user.role.librarian", Locale.forLanguageTag("pt-BR"))).thenReturn("Librarian");

        AppUser result = service().createAdmin(request);

        assertThat(result.getEmail()).isEqualTo("librarian@example.test");
        assertThat(result.getRole()).isEqualTo(Role.LIBRARIAN);
        assertThat(result.getPasswordHash()).isEqualTo("encoded-pass");
        verify(emailService).enviarSenhaInicialAdmin(
                "librarian@example.test",
                "Librarian",
                "initial-pass",
                Locale.forLanguageTag("pt-BR"));
    }

    @Test
    void createAdminRejectsStudentRole() {
        UserRequest request = UserRequest.builder()
                .email("student@example.test")
                .password("initial-pass")
                .role(Role.STUDENT)
                .build();
        when(passwordEncoder.encode("initial-pass")).thenReturn("encoded-pass");

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().createAdmin(request))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("user.cannot-register-student-here"));
        verify(appUserRepository, never()).save(any());
        verify(emailService, never()).enviarSenhaInicialAdmin(any(), any(), any(), any());
    }

    @Test
    void createAdminRejectsDuplicateEmailBeforeEncodingPassword() {
        UserRequest request = UserRequest.builder()
                .email("admin@example.test")
                .password("initial-pass")
                .role(Role.ADMIN)
                .build();
        when(appUserRepository.existsByEmail("admin@example.test")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().createAdmin(request))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("user.email.in-use"));
        verify(passwordEncoder, never()).encode(any());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void updateUserChangesEmailAndPasswordWhenPasswordIsProvided() {
        UUID id = UUID.randomUUID();
        AppUser existing = AppUser.builder()
                .id(id)
                .email("old@example.test")
                .passwordHash("old-hash")
                .role(Role.ADMIN)
                .build();
        UserRequest request = UserRequest.builder()
                .email("new@example.test")
                .password("new-pass")
                .build();
        when(appUserRepository.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service().updateUser(id, request);

        assertThat(result.getEmail()).isEqualTo("new@example.test");
        assertThat(result.getPasswordHash()).isEqualTo("new-hash");
        verify(appUserRepository).existsByEmail("new@example.test");
    }

    @Test
    void updateUserRejectsMissingUser() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().updateUser(id, UserRequest.builder()
                        .email("new@example.test")
                        .password("new-pass")
                        .build()))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("user.not-found"));
    }

    @Test
    void changePasswordUpdatesLoggedUserPassword() {
        AppUser loggedUser = AppUser.builder()
                .email("admin@example.test")
                .passwordHash("old-hash")
                .role(Role.ADMIN)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findByEmailOrRegistrationNumber("admin@example.test", "admin@example.test"))
                .thenReturn(Optional.of(loggedUser));
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");

        service().changePassword(new ChangePasswordRequest(null, "old-pass", "new-pass"));

        verify(appUserRepository).save(appUserCaptor.capture());
        assertThat(appUserCaptor.getValue().getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        AppUser loggedUser = AppUser.builder()
                .email("admin@example.test")
                .passwordHash("old-hash")
                .role(Role.ADMIN)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findByEmailOrRegistrationNumber("admin@example.test", "admin@example.test"))
                .thenReturn(Optional.of(loggedUser));
        when(passwordEncoder.matches("wrong-pass", "old-hash")).thenReturn(false);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().changePassword(
                        new ChangePasswordRequest(null, "wrong-pass", "new-pass")))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("user.password.incorrect"));
        verify(appUserRepository, never()).save(any());
    }

    private AppUserService service() {
        return new AppUserService(emailService, passwordEncoder, appUserRepository, messages);
    }

    private static void setLoggedUser(String username) {
        User principal = (User) User.withUsername(username)
                .password("ignored")
                .roles("ADMIN")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
