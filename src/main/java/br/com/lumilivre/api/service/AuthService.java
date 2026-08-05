package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.domain.policy.PasswordPolicy;
import br.com.lumilivre.api.dto.auth.LoginResponse;
import br.com.lumilivre.api.dto.auth.ResetPasswordTokenRequest;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.PasswordResetToken;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.PasswordResetTokenRepository;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.LoginAttemptService;
import br.com.lumilivre.api.service.infra.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Hash BCrypt fixo e válido usado só para gastar o mesmo tempo de um
    // matches() real quando o usuário NÃO existe — iguala o timing e derrota a
    // enumeração de contas por tempo de resposta. (Não é credencial de ninguém.)
    private static final String DUMMY_HASH =
            "$2a$10$fHJ73JQxR0RhvAJVYA8ZtuoNyfup0aE1WML5B82x.VSkQigYppugK";

    // 30 min: tempo de abrir o e-mail e trocar a senha, curto o bastante para
    // que um link vazado numa caixa esquecida não sirva mais.
    static final int TOKEN_TTL_MINUTES = 30;

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
        // Trava por conta antes de qualquer verificação de senha.
        if (loginAttemptService.isBlocked(username)) {
            throw new LockedException("auth.login.error.too-many-attempts");
        }

        Optional<AppUser> found = appUserRepository.findByEmailOrRegistrationNumber(username, username);
        if (found.isEmpty()) {
            // Roda um matches() dummy para gastar o mesmo tempo de um
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

        // Status só depois da senha conferir: quem não sabe a senha continua
        // recebendo "credenciais inválidas" e não descobre quais contas existem.
        assertAccountUsable(appUser);

        loginAttemptService.recordSuccess(username);

        // A "primeira senha" agora vem de uma flag persistida, não de
        // comparação de string (que quebrava se a nova senha coincidisse com a matrícula).
        boolean isInitialPassword = Boolean.TRUE.equals(appUser.getMustChangePassword());

        return new AuthenticatedLogin(appUser, issueToken(appUser), isInitialPassword);
    }

    /**
     * Emite um JWT novo carimbado com a geração de tokens atual da conta. Usado
     * no login e depois da troca de senha — que incrementa a geração e precisa
     * devolver um token válido por construção, senão o usuário sairia deslogado
     * do próprio dispositivo onde trocou a senha.
     */
    public String issueToken(AppUser appUser) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));

        User userDetails = new User(appUser.getEmail(), appUser.getPasswordHash(), authorities);
        int tokenVersion = appUser.getTokenVersion() != null ? appUser.getTokenVersion() : 0;
        return jwtUtil.generateToken(userDetails, tokenVersion);
    }

    /**
     * Conta desativada, bloqueada ou excluída não entra, mesmo com a senha certa.
     * {@link DisabledException} para os dois casos porque {@link LockedException}
     * já significa "trava temporária por tentativas" (429) neste projeto.
     */
    private void assertAccountUsable(AppUser appUser) {
        if (appUser.getDeletedAt() != null || !Boolean.TRUE.equals(appUser.getActive())) {
            throw new DisabledException("auth.login.error.account-disabled");
        }
        if (Boolean.TRUE.equals(appUser.getLocked())) {
            throw new DisabledException("auth.login.error.account-locked");
        }
    }

    @Transactional
    public void solicitarResetSenha(String email) {
        Optional<AppUser> appUserOpt = appUserRepository.findByEmail(email);

        if (appUserOpt.isPresent()) {
            AppUser appUser = appUserOpt.get();
            String token = UUID.randomUUID().toString();

            // Um pedido novo invalida o anterior (SEC-23): dois links vivos ao
            // mesmo tempo dobram a janela de ataque e a tabela tem UNIQUE em
            // app_user_id, então o insert seguinte precisa da limpeza + flush.
            passwordResetTokenRepository.deleteByAppUserId(appUser.getId());
            passwordResetTokenRepository.flush();

            PasswordResetToken passwordResetToken = new PasswordResetToken(token, appUser, TOKEN_TTL_MINUTES);
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

        PasswordPolicy.validate(
                request.getNewPassword(),
                passwordEncoder.matches(request.getNewPassword(), appUser.getPasswordHash()),
                personalDataOf(appUser));

        appUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Senha escolhida pelo próprio usuário: a troca obrigatória deixa de valer.
        appUser.setMustChangePassword(false);
        // Quem chegou aqui provavelmente perdeu a conta para alguém; derrubar
        // todas as sessões é o ponto do reset.
        appUser.revokeIssuedTokens();
        appUserRepository.save(appUser);

        passwordResetTokenRepository.delete(passwordResetToken);
    }

    /**
     * Logout de verdade: empurra o corte de revogação e mata o token que o
     * cliente ainda tem em mãos. Sem isso, "sair" era só apagar o token do
     * navegador — o token continuava aceito até vencer.
     *
     * @return o e-mail da conta, para a trilha de acessos
     */
    @Transactional
    public String logout(String username) {
        AppUser appUser = appUserRepository.findAliveByLogin(username)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.logged-in-not-found"));

        appUser.revokeIssuedTokens();
        appUserRepository.save(appUser);
        return appUser.getEmail();
    }

    /**
     * Valores que a senha não pode reproduzir. Todos são conhecidos por quem
     * convive com o leitor (matrícula na carteirinha, nome na chamada).
     */
    static String[] personalDataOf(AppUser appUser) {
        Reader reader = appUser.getReader();
        if (reader == null) {
            return new String[] { appUser.getEmail() };
        }
        return new String[] {
                appUser.getEmail(),
                reader.getRegistrationNumber(),
                reader.getCpf(),
                reader.getEmail(),
                reader.getFullName()
        };
    }

    private record AuthenticatedLogin(AppUser appUser, String token, boolean initialPassword) {}
}
