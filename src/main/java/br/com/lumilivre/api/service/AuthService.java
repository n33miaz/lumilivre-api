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

import br.com.lumilivre.api.dto.auth.LoginRequest;
import br.com.lumilivre.api.dto.auth.LoginResponse;
import br.com.lumilivre.api.dto.auth.MudarSenhaTokenRequest;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
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

    public LoginResponse login(LoginRequest dto) {
        AppUser appUser = appUserRepository.findByEmailOrAluno_Matricula(dto.getUser(), dto.getUser())
                .orElseThrow(() -> new RecursoNaoEncontradoException("UsuÃ¡rio nÃ£o encontrado"));

        if (!passwordEncoder.matches(dto.getSenha(), appUser.getSenha())) {
            throw new BadCredentialsException("Senha incorreta");
        }

        boolean isInitialPassword = false;
        if (appUser.getAluno() != null) {
            String matricula = appUser.getAluno().getMatricula();
            if (dto.getSenha().equals(matricula)) {
                isInitialPassword = true;
            }
        } else if (dto.getSenha().equals(appUser.getEmail())) {
            isInitialPassword = true;
        }

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));

        User userDetails = new User(appUser.getEmail(), appUser.getSenha(), authorities);
        String token = jwtUtil.generateToken(userDetails);

        return new LoginResponse(appUser, token, isInitialPassword);
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
            emailService.enviarEmailResetSenha(appUser.getEmail(), linkReset);
        }
    }

    public boolean validarTokenReset(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        return tokenOpt.isPresent() && !tokenOpt.get().isExpirado();
    }

    @Transactional
    public void mudarSenhaComToken(MudarSenhaTokenRequest dto) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token invÃ¡lido ou nÃ£o encontrado."));

        if (passwordResetToken.isExpirado()) {
            throw new IllegalArgumentException("Token expirado.");
        }

        AppUser appUser = passwordResetToken.getUsuario();
        appUser.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        appUserRepository.save(appUser);

        passwordResetTokenRepository.delete(passwordResetToken);
    }
}
