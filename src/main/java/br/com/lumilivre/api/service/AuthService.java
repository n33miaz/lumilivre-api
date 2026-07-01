package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
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
import br.com.lumilivre.api.service.infra.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

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
                .build();
    }

    private AuthenticatedLogin authenticate(String username, String password) {
        AppUser appUser = appUserRepository.findByEmailOrRegistrationNumber(username, username)
                .orElseThrow(() -> new BadCredentialsException("auth.login.error.invalid-credentials"));

        if (!passwordEncoder.matches(password, appUser.getPasswordHash())) {
            throw new BadCredentialsException("auth.login.error.invalid-credentials");
        }

        boolean isInitialPassword = false;
        if (appUser.getReader() != null) {
            String matricula = appUser.getReader().getRegistrationNumber();
            if (password.equals(matricula)) {
                isInitialPassword = true;
            }
        } else if (password.equals(appUser.getEmail())) {
            isInitialPassword = true;
        }

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
        appUserRepository.save(appUser);

        passwordResetTokenRepository.delete(passwordResetToken);
    }

    private record AuthenticatedLogin(AppUser appUser, String token, boolean initialPassword) {}
}
