package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.lumilivre.api.dto.auth.LoginRequest;
import br.com.lumilivre.api.dto.auth.MudarSenhaTokenRequest;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.PasswordResetToken;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.PasswordResetTokenRepository;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.infra.EmailService;

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
    private EmailService emailService;

    @InjectMocks
    private AuthService service;

    @Test
    void loginDeveAutenticarUsuarioAdminComEmail() {
        AppUser usuario = usuario(Role.ADMIN, null);
        when(usuarioRepository.findByEmailOrAluno_Matricula("admin@lumilivre.test", "admin@lumilivre.test"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-segura", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        var response = service.login(new LoginRequest("admin@lumilivre.test", "senha-segura"));

        assertThat(response.getEmail()).isEqualTo("admin@lumilivre.test");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.isInitialPassword()).isFalse();
    }

    @Test
    void loginDeveMarcarSenhaInicialQuandoAlunoUsaMatricula() {
        Student aluno = new Student();
        aluno.setMatricula("12345");

        AppUser usuario = usuario(Role.STUDENT, aluno);
        when(usuarioRepository.findByEmailOrAluno_Matricula("12345", "12345")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("12345", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        var response = service.login(new LoginRequest("12345", "12345"));

        assertThat(response.getMatriculaAluno()).isEqualTo("12345");
        assertThat(response.isInitialPassword()).isTrue();
    }

    @Test
    void loginDeveFalharQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByEmailOrAluno_Matricula("ninguemm", "ninguemm")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ninguemm", "senha")))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void loginDeveFalharQuandoSenhaNaoConfere() {
        AppUser usuario = usuario(Role.LIBRARIAN, null);
        when(usuarioRepository.findByEmailOrAluno_Matricula("biblioteca@lumilivre.test", "biblioteca@lumilivre.test"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("biblioteca@lumilivre.test", "errada")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void solicitarResetSenhaDeveSalvarTokenEEnviarEmailQuandoUsuarioExiste() {
        AppUser usuario = usuario(Role.ADMIN, null);
        when(usuarioRepository.findByEmail("admin@lumilivre.test")).thenReturn(Optional.of(usuario));

        service.solicitarResetSenha("admin@lumilivre.test");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(tokenCaptor.getValue().isExpirado()).isFalse();

        verify(emailService).enviarEmailResetSenha(
                eq("admin@lumilivre.test"),
                contains(tokenCaptor.getValue().getToken()));
    }

    @Test
    void solicitarResetSenhaDeveSerSilenciosoQuandoEmailNaoExiste() {
        when(usuarioRepository.findByEmail("desconhecido@lumilivre.test")).thenReturn(Optional.empty());

        service.solicitarResetSenha("desconhecido@lumilivre.test");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarEmailResetSenha(any(), any());
    }

    @Test
    void validarTokenResetDeveRetornarTrueSomenteParaTokenExistenteENaoExpirado() {
        when(tokenRepository.findByToken("valido")).thenReturn(Optional.of(tokenReset(LocalDateTime.now().plusMinutes(5))));
        when(tokenRepository.findByToken("expirado")).thenReturn(Optional.of(tokenReset(LocalDateTime.now().minusMinutes(1))));
        when(tokenRepository.findByToken("ausente")).thenReturn(Optional.empty());

        assertThat(service.validarTokenReset("valido")).isTrue();
        assertThat(service.validarTokenReset("expirado")).isFalse();
        assertThat(service.validarTokenReset("ausente")).isFalse();
    }

    @Test
    void mudarSenhaComTokenDeveAtualizarSenhaEInvalidarToken() {
        PasswordResetToken tokenReset = tokenReset(LocalDateTime.now().plusMinutes(5));
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(tokenReset));
        when(passwordEncoder.encode("nova-senha")).thenReturn("novo-hash");

        service.mudarSenhaComToken(new MudarSenhaTokenRequest("token-123", "nova-senha"));

        assertThat(tokenReset.getUsuario().getSenha()).isEqualTo("novo-hash");
        verify(usuarioRepository).save(tokenReset.getUsuario());
        verify(tokenRepository).delete(tokenReset);
    }

    @Test
    void mudarSenhaComTokenDeveBloquearTokenExpirado() {
        PasswordResetToken tokenReset = tokenReset(LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("token-123")).thenReturn(Optional.of(tokenReset));

        assertThatThrownBy(() -> service.mudarSenhaComToken(new MudarSenhaTokenRequest("token-123", "nova-senha")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirado");

        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }

    private static PasswordResetToken tokenReset(LocalDateTime expiration) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("token-123");
        token.setUsuario(usuario(Role.ADMIN, null));
        token.setDataExpiracao(expiration);
        return token;
    }

    private static AppUser usuario(Role role, Student aluno) {
        AppUser usuario = new AppUser();
        usuario.setId(1);
        usuario.setEmail(role == Role.STUDENT ? "aluno@lumilivre.test" : role.name().toLowerCase() + "@lumilivre.test");
        usuario.setSenha("hash");
        usuario.setRole(role);
        usuario.setAluno(aluno);
        return usuario;
    }
}
