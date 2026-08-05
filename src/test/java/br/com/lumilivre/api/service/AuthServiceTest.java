package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.PasswordPolicy.PasswordPolicyViolationException;
import br.com.lumilivre.api.dto.auth.ResetPasswordTokenRequest;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.PasswordResetToken;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.PasswordResetTokenRepository;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.LoginAttemptService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @Mock
    private MessageResolver messages;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService service;

    @Test
    void loginDeveAutenticarUsuarioAdminComEmail() {
        AppUser usuario = usuario(Role.ADMIN, null);
        when(usuarioRepository.findByEmailOrRegistrationNumber("admin@lumilivre.test", "admin@lumilivre.test"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(any(UserDetails.class), anyInt())).thenReturn("jwt-token");

        var response = service.login("admin@lumilivre.test", "senha-segura");

        assertThat(response.getEmail()).isEqualTo("admin@lumilivre.test");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.isInitialPasswordChange()).isFalse();
    }

    @Test
    void loginDeveMarcarSenhaInicialQuandoFlagAtiva() {
        Reader leitor = new Reader();
        leitor.setRegistrationNumber("12345");

        AppUser usuario = usuario(Role.READER, leitor);
        usuario.setMustChangePassword(true);
        when(usuarioRepository.findByEmailOrRegistrationNumber("12345", "12345")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("12345", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(any(UserDetails.class), anyInt())).thenReturn("jwt-token");

        var response = service.login("12345", "12345");

        assertThat(response.getReaderRegistrationNumber()).isEqualTo("12345");
        assertThat(response.isInitialPasswordChange()).isTrue();
    }

    @Test
    void loginDeveFalharQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByEmailOrRegistrationNumber("ninguemm", "ninguemm")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ninguemm", "senha"))
                .isInstanceOf(BadCredentialsException.class);

        // Um matches() dummy roda mesmo sem usuário (timing constante)
        verify(passwordEncoder).matches(eq("senha"), any());
        verify(loginAttemptService).recordFailure("ninguemm");
        verify(jwtUtil, never()).generateToken(any(), anyInt());
    }

    @Test
    void loginDeveFalharQuandoSenhaNaoConfere() {
        AppUser usuario = usuario(Role.LIBRARIAN, null);
        when(usuarioRepository.findByEmailOrRegistrationNumber("biblioteca@lumilivre.test", "biblioteca@lumilivre.test"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("biblioteca@lumilivre.test", "errada"))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordFailure("biblioteca@lumilivre.test");
        verify(jwtUtil, never()).generateToken(any(), anyInt());
    }

    @Test
    void loginDeveBloquearQuandoContaTravadaPorTentativas() {
        when(loginAttemptService.isBlocked("admin@lumilivre.test")).thenReturn(true);

        assertThatThrownBy(() -> service.login("admin@lumilivre.test", "senha"))
                .isInstanceOf(LockedException.class);

        verify(usuarioRepository, never()).findByEmailOrRegistrationNumber(any(), any());
        verify(jwtUtil, never()).generateToken(any(), anyInt());
    }

    @Test
    void loginDeveRecusarContaDesativada() {
        AppUser usuario = usuario(Role.LIBRARIAN, null);
        usuario.setActive(false);
        when(usuarioRepository.findByEmailOrRegistrationNumber("librarian@lumilivre.test", "librarian@lumilivre.test"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login("librarian@lumilivre.test", "senha-segura"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("auth.login.error.account-disabled");

        verify(jwtUtil, never()).generateToken(any(), anyInt());
        // Senha correta: não é falha de credencial, então nada de contar tentativa.
        verify(loginAttemptService, never()).recordSuccess(any());
    }

    @Test
    void loginDeveRecusarContaBloqueada() {
        AppUser usuario = usuario(Role.LIBRARIAN, null);
        usuario.setLocked(true);
        when(usuarioRepository.findByEmailOrRegistrationNumber("librarian@lumilivre.test", "librarian@lumilivre.test"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login("librarian@lumilivre.test", "senha-segura"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("auth.login.error.account-locked");

        verify(jwtUtil, never()).generateToken(any(), anyInt());
    }

    @Test
    void loginDeveRecusarContaExcluida() {
        AppUser usuario = usuario(Role.LIBRARIAN, null);
        usuario.setDeletedAt(OffsetDateTime.now().minusDays(1));
        when(usuarioRepository.findByEmailOrRegistrationNumber("librarian@lumilivre.test", "librarian@lumilivre.test"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login("librarian@lumilivre.test", "senha-segura"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("auth.login.error.account-disabled");

        verify(jwtUtil, never()).generateToken(any(), anyInt());
    }

    @Test
    void logoutDeveIncrementarAGeracaoDeTokens() {
        AppUser usuario = usuario(Role.ADMIN, null);
        usuario.setTokenVersion(7);
        when(usuarioRepository.findAliveByLogin("admin@lumilivre.test")).thenReturn(Optional.of(usuario));

        String email = service.logout("admin@lumilivre.test");

        assertThat(email).isEqualTo("admin@lumilivre.test");
        assertThat(usuario.getTokenVersion()).isEqualTo(8);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void logoutDeveFalharQuandoContaNaoExisteMais() {
        when(usuarioRepository.findAliveByLogin("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logout("fantasma"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void solicitarResetSenhaDeveInvalidarTokensAnteriores() {
        AppUser usuario = usuario(Role.ADMIN, null);
        when(usuarioRepository.findByEmail("admin@lumilivre.test")).thenReturn(Optional.of(usuario));

        service.solicitarResetSenha("admin@lumilivre.test");

        // Ordem importa: apagar + flush antes do insert, senão o UNIQUE de
        // app_user_id em password_reset_token estoura no segundo pedido.
        InOrder ordem = inOrder(tokenRepository);
        ordem.verify(tokenRepository).deleteByAppUserId(usuario.getId());
        ordem.verify(tokenRepository).flush();
        ordem.verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void mudarSenhaComTokenDeveRecusarSenhaFraca() {
        PasswordResetToken tokenReset = tokenReset(OffsetDateTime.now().plusMinutes(5));
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(tokenReset));

        assertThatThrownBy(() -> service.resetPasswordWithToken(
                new ResetPasswordTokenRequest("token-123", "123456")))
                .isInstanceOf(PasswordPolicyViolationException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void mudarSenhaComTokenDeveRevogarTokensJaEmitidos() {
        PasswordResetToken tokenReset = tokenReset(OffsetDateTime.now().plusMinutes(5));
        tokenReset.getAppUser().setTokenVersion(7);
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(tokenReset));
        when(passwordEncoder.encode("nova-senha")).thenReturn("novo-hash");

        service.resetPasswordWithToken(new ResetPasswordTokenRequest("token-123", "nova-senha"));

        assertThat(tokenReset.getAppUser().getTokenVersion()).isEqualTo(8);
    }

    @Test
    void personalDataDeveIncluirDadosDoLeitorQuandoHouver() {
        Reader leitor = new Reader();
        leitor.setRegistrationNumber("2024001");
        leitor.setCpf("12345678901");
        leitor.setFullName("Maria Souza");
        AppUser usuario = usuario(Role.READER, leitor);

        assertThat(AuthService.personalDataOf(usuario))
                .contains("2024001", "12345678901", "Maria Souza", "leitor@lumilivre.test");
    }

    @Test
    void solicitarResetSenhaDeveSalvarTokenEPublicarNoOutbox() {
        // O envio ficava dentro desta transacao: MailAuthenticationException e
        // RuntimeException e derrubava tudo, entao o token novo se perdia E a
        // invalidacao do anterior era desfeita -- recuperacao de senha nao
        // funcionava com SMTP ruim. Agora o e-mail e um evento persistido junto
        // com o token, entregue depois e com retry.
        AppUser usuario = usuario(Role.ADMIN, null);
        when(usuarioRepository.findByEmail("admin@lumilivre.test")).thenReturn(Optional.of(usuario));

        service.solicitarResetSenha("admin@lumilivre.test");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getAppUser()).isSameAs(usuario);
        assertThat(tokenCaptor.getValue().isExpired()).isFalse();

        verify(outboxPublisher).publish(
                eq(EventType.PASSWORD_RESET),
                eq("admin@lumilivre.test"),
                any(),
                contains(tokenCaptor.getValue().getToken()),
                any(Locale.class));
    }

    @Test
    void solicitarResetSenhaDeveSerSilenciosoQuandoEmailNaoExiste() {
        when(usuarioRepository.findByEmail("desconhecido@lumilivre.test")).thenReturn(Optional.empty());

        service.solicitarResetSenha("desconhecido@lumilivre.test");

        verify(tokenRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void solicitarResetSenhaNaoEstouraQuandoOEmailVemAusenteOuVazio() {
        // 204 em qualquer caminho: e o que fecha o oraculo de enumeracao. Antes,
        // endereco inexistente devolvia 204 e conta real devolvia 500.
        service.solicitarResetSenha(null);
        service.solicitarResetSenha("   ");

        verify(usuarioRepository, never()).findByEmail(any());
        verify(tokenRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void validarTokenResetDeveRetornarTrueSomenteParaTokenExistenteENaoExpirado() {
        when(tokenRepository.findByToken("valido")).thenReturn(Optional.of(tokenReset(OffsetDateTime.now().plusMinutes(5))));
        when(tokenRepository.findByToken("expirado")).thenReturn(Optional.of(tokenReset(OffsetDateTime.now().minusMinutes(1))));
        when(tokenRepository.findByToken("ausente")).thenReturn(Optional.empty());

        assertThat(service.validarTokenReset("valido")).isTrue();
        assertThat(service.validarTokenReset("expirado")).isFalse();
        assertThat(service.validarTokenReset("ausente")).isFalse();
    }

    @Test
    void mudarSenhaComTokenDeveAtualizarSenhaEInvalidarToken() {
        PasswordResetToken tokenReset = tokenReset(OffsetDateTime.now().plusMinutes(5));
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(tokenReset));
        when(passwordEncoder.encode("nova-senha")).thenReturn("novo-hash");

        service.resetPasswordWithToken(new ResetPasswordTokenRequest("token-123", "nova-senha"));

        assertThat(tokenReset.getAppUser().getPasswordHash()).isEqualTo("novo-hash");
        verify(usuarioRepository).save(tokenReset.getAppUser());
        verify(tokenRepository).delete(tokenReset);
    }

    @Test
    void mudarSenhaComTokenDeveBloquearTokenExpirado() {
        PasswordResetToken tokenReset = tokenReset(OffsetDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(tokenReset));

        assertThatThrownBy(() -> service.resetPasswordWithToken(new ResetPasswordTokenRequest("token-123", "nova-senha")))
                .isInstanceOf(BusinessRuleException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }

    private static PasswordResetToken tokenReset(OffsetDateTime expiration) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("token-123");
        token.setAppUser(usuario(Role.ADMIN, null));
        token.setExpiresAt(expiration);
        token.setCreatedAt(OffsetDateTime.now());
        return token;
    }

    private static AppUser usuario(Role role, Reader leitor) {
        AppUser usuario = new AppUser();
        usuario.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        usuario.setEmail(role == Role.READER ? "leitor@lumilivre.test" : role.name().toLowerCase() + "@lumilivre.test");
        usuario.setPasswordHash("hash");
        usuario.setRole(role);
        usuario.setReader(leitor);
        // Espelha o NOT NULL DEFAULT da V7: conta vinda do banco nunca tem flag
        // nula, e a verificação de status falha fechado.
        usuario.setActive(true);
        usuario.setLocked(false);
        usuario.setTokenVersion(0);
        return usuario;
    }
}
