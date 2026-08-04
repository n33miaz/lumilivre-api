package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.auth.LoginResponse;
import br.com.lumilivre.api.dto.auth.ResetPasswordTokenRequest;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.PasswordResetToken;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.PasswordResetTokenRepository;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.LoginAttemptService;
import br.com.lumilivre.api.service.infra.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    // SEC-12: hash BCrypt fixo e válido usado só para gastar o mesmo tempo de um
    // matches() real quando o usuário NÃO existe — iguala o timing e derrota a
    // enumeração de contas por tempo de resposta. (Não é credencial de ninguém.)
    private static final String DUMMY_HASH =
            "$2a$10$fHJ73JQxR0RhvAJVYA8ZtuoNyfup0aE1WML5B82x.VSkQigYppugK";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;

    public LoginResponse login(String username, String password) {
        AuthenticatedLogin login = authenticate(username, password);
        return LoginResponse.builder()
                .id(login.appUser().getId())
                .email(login.appUser().getEmail())
                .role(login.appUser().getRole().name())
                .readerRegistrationNumber(
                        login.appUser().getReader() != null
                                ? login.appUser().getReader().getRegistrationNumber()
                                : null)
                .token(login.token())
                .initialPasswordChange(login.initialPassword())
                .guidedTourCompleted(Boolean.TRUE.equals(login.appUser().getGuidedTourCompleted()))
                .build();
    }

    private AuthenticatedLogin authenticate(String username, String password) {
        // SEC-05: trava por conta antes de qualquer verificação de senha.
        if (loginAttemptService.isBlocked(username)) {
            throw new LockedException("auth.login.error.too-many-attempts");
        }

        Optional<AppUser> found = appUserRepository.findByEmailOrRegistrationNumber(username, username);
        if (found.isEmpty()) {
            // SEC-12: roda um matches() dummy para gastar o mesmo tempo de um
            // login real (evita enumeração por timing). Resultado ignorado.
            passwordEncoder.matches(password, DUMMY_HASH);
            loginAttemptService.recordFailure(username);
            throw new BadCredentialsException("auth.login.error.invalid-credentials");
        }

        AppUser appUser = found.get();

        if (!passwordEncoder.matches(password, appUser.getPasswordHash())) {
            loginAttemptService.recordFailure(username);
            throw new BadCredentialsException("auth.login.error.invalid-credentials");
        }

        loginAttemptService.recordSuccess(username);

        // WS-10/SEC-03: a "primeira senha" agora vem de uma flag persistida, não de
        // comparação de string (que quebrava se a nova senha coincidisse com a matrícula).
        boolean isInitialPassword = Boolean.TRUE.equals(appUser.getMustChangePassword());

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));

        User userDetails = new User(appUser.getEmail(), appUser.getPasswordHash(), authorities);
        String token = jwtUtil.generateToken(userDetails);

        return new AuthenticatedLogin(appUser, token, isInitialPassword);
    }

    @Transactional
    public void solicitarResetSenha(String email) {
        Optional<AppUser> appUserOpt = appUserRepository.findByEmail(email);

        if (appUserOpt.isPresent()) {
            AppUser appUser = appUserOpt.get();
            String token = UUID.randomUUID().toString();

            PasswordResetToken passwordResetToken = new PasswordResetToken(token, appUser, 30);
            passwordResetTokenRepository.save(passwordResetToken);

            String linkReset = "https://www.lumilivre.com.br/mudar-senha?token=" + token;
            String preferredLocale = appUser.getPreferredLocale() != null ? appUser.getPreferredLocale() : "pt-BR";
            emailService.enviarEmailResetSenha(appUser.getEmail(), linkReset,
                    java.util.Locale.forLanguageTag(preferredLocale));
        }
    }

    public boolean validarTokenReset(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        return tokenOpt.isPresent() && !tokenOpt.get().isExpired();
    }

    @Transactional
    public void resetPasswordWithToken(ResetPasswordTokenRequest request) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> BusinessRuleException.ofKey("auth.password-reset.error.token-invalid"));

        if (passwordResetToken.isExpired()) {
            throw BusinessRuleException.ofKey("auth.password-reset.error.token-invalid");
        }

        AppUser appUser = passwordResetToken.getAppUser();
        appUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Senha escolhida pelo próprio usuário: a troca obrigatória deixa de valer.
        appUser.setMustChangePassword(false);
        appUserRepository.save(appUser);

        passwordResetTokenRepository.delete(passwordResetToken);
    }

    private record AuthenticatedLogin(AppUser appUser, String token, boolean initialPassword) {}
}
