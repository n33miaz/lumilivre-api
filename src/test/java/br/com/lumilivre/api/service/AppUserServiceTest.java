package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.PasswordPolicy.PasswordPolicyViolationException;
import br.com.lumilivre.api.dto.auth.ChangePasswordRequest;
import br.com.lumilivre.api.dto.user.UserRequest;
import br.com.lumilivre.api.dto.user.UserStatusRequest;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Reader;
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
    void createAdminRejectsReaderRole() {
        UserRequest request = UserRequest.builder()
                .email("reader@example.test")
                .password("initial-pass")
                .role(Role.READER)
                .build();
        when(passwordEncoder.encode("initial-pass")).thenReturn("encoded-pass");

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().createAdmin(request))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("user.cannot-register-reader-here"));
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

    @Test
    void changePasswordRejectsWeakPassword() {
        AppUser loggedUser = AppUser.builder()
                .email("admin@example.test")
                .passwordHash("old-hash")
                .role(Role.ADMIN)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findByEmailOrRegistrationNumber("admin@example.test", "admin@example.test"))
                .thenReturn(Optional.of(loggedUser));
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);

        assertThatExceptionOfType(PasswordPolicyViolationException.class)
                .isThrownBy(() -> service().changePassword(
                        new ChangePasswordRequest(null, "old-pass", "123456")))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("validation.password.too-short"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsPasswordDerivedFromRegistrationNumber() {
        Reader reader = new Reader();
        reader.setRegistrationNumber("2024001");
        reader.setFullName("Maria Souza");
        AppUser loggedUser = AppUser.builder()
                .email("reader@example.test")
                .passwordHash("old-hash")
                .role(Role.READER)
                .reader(reader)
                .build();
        setLoggedUser("2024001");
        when(appUserRepository.findByEmailOrRegistrationNumber("2024001", "2024001"))
                .thenReturn(Optional.of(loggedUser));
        when(passwordEncoder.matches("2024001", "old-hash")).thenReturn(true);

        assertThatExceptionOfType(PasswordPolicyViolationException.class)
                .isThrownBy(() -> service().changePassword(
                        new ChangePasswordRequest("2024001", "2024001", "senha2024001")))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("validation.password.personal-data"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void changePasswordRevokesTokensIssuedWithTheOldPassword() {
        AppUser loggedUser = AppUser.builder()
                .email("admin@example.test")
                .passwordHash("old-hash")
                .role(Role.ADMIN)
                .tokenVersion(3)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findByEmailOrRegistrationNumber("admin@example.test", "admin@example.test"))
                .thenReturn(Optional.of(loggedUser));
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("chuva-de-papel-77")).thenReturn("new-hash");

        AppUser result = service().changePassword(
                new ChangePasswordRequest(null, "old-pass", "chuva-de-papel-77"));

        assertThat(result.getTokenVersion()).isEqualTo(4);
        assertThat(result.getMustChangePassword()).isFalse();
    }

    @Test
    void setStatusDeactivatesAccountAndRevokesItsTokens() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("librarian@example.test")
                .role(Role.LIBRARIAN)
                .tokenVersion(3)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service().setStatus(id, UserStatusRequest.builder().active(false).build());

        assertThat(result.getActive()).isFalse();
        assertThat(result.getLocked()).isFalse();
        assertThat(result.getTokenVersion()).isEqualTo(4);
    }

    @Test
    void setStatusLocksAccountAndRevokesItsTokens() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("librarian@example.test")
                .role(Role.LIBRARIAN)
                .tokenVersion(3)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service().setStatus(id, UserStatusRequest.builder().locked(true).build());

        assertThat(result.getLocked()).isTrue();
        assertThat(result.getActive()).isTrue();
        assertThat(result.getTokenVersion()).isEqualTo(4);
    }

    @Test
    void setStatusReactivationDoesNotBumpTheTokenVersion() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("librarian@example.test")
                .role(Role.LIBRARIAN)
                .active(false)
                .tokenVersion(3)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service().setStatus(id, UserStatusRequest.builder().active(true).build());

        assertThat(result.getActive()).isTrue();
        assertThat(result.getTokenVersion()).isEqualTo(3);
    }

    @Test
    void setStatusRefusesToDisableTheCallersOwnAccount() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("admin@example.test")
                .role(Role.ADMIN)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().setStatus(id, UserStatusRequest.builder().active(false).build()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("user.status.cannot-disable-self"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void setStatusRefusesToDisableTheLastUsableAdmin() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("owner@example.test")
                .role(Role.ADMIN)
                .build();
        setLoggedUser("other-admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));
        when(appUserRepository.countUsableAdmins()).thenReturn(1L);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().setStatus(id, UserStatusRequest.builder().locked(true).build()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("user.status.cannot-disable-last-admin"));
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void setStatusAllowsDisablingAnAdminWhenAnotherOneRemains() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("owner@example.test")
                .role(Role.ADMIN)
                .tokenVersion(3)
                .build();
        setLoggedUser("other-admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));
        when(appUserRepository.countUsableAdmins()).thenReturn(2L);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service().setStatus(id, UserStatusRequest.builder().active(false).build());

        assertThat(result.getActive()).isFalse();
    }

    @Test
    void setStatusRejectsEmptyBody() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().setStatus(UUID.randomUUID(), new UserStatusRequest()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("user.status.nothing-to-change"));
        verify(appUserRepository, never()).findById(any());
    }

    @Test
    void excluirRefusesToDeleteTheCallersOwnAccount() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("admin@example.test")
                .role(Role.ADMIN)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().excluir(id))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("user.status.cannot-disable-self"));
        verify(appUserRepository, never()).delete(any());
    }

    @Test
    void excluirRefusesToDeleteTheLastUsableAdmin() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("owner@example.test")
                .role(Role.ADMIN)
                .build();
        setLoggedUser("other-admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));
        when(appUserRepository.countUsableAdmins()).thenReturn(1L);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().excluir(id))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("user.status.cannot-disable-last-admin"));
        verify(appUserRepository, never()).delete(any());
    }

    @Test
    void excluirDeletesAnotherAccount() {
        UUID id = UUID.randomUUID();
        AppUser target = AppUser.builder()
                .id(id)
                .email("librarian@example.test")
                .role(Role.LIBRARIAN)
                .build();
        setLoggedUser("admin@example.test");
        when(appUserRepository.findById(id)).thenReturn(Optional.of(target));

        service().excluir(id);

        verify(appUserRepository).delete(target);
    }

    @Test
    void setStatusRejectsMissingUser() {
        UUID id = UUID.randomUUID();
        when(appUserRepository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().setStatus(id, UserStatusRequest.builder().active(false).build()))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("user.not-found"));
    }

    @Test
    void updateUserWithNewPasswordRevokesExistingSessions() {
        UUID id = UUID.randomUUID();
        AppUser existing = AppUser.builder()
                .id(id)
                .email("old@example.test")
                .passwordHash("old-hash")
                .role(Role.ADMIN)
                .tokenVersion(3)
                .build();
        when(appUserRepository.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = service().updateUser(id, UserRequest.builder()
                .email("old@example.test")
                .password("new-pass")
                .build());

        assertThat(result.getTokenVersion()).isEqualTo(4);
        assertThat(result.getMustChangePassword()).isTrue();
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
