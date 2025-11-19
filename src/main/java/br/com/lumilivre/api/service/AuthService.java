package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
import br.com.lumilivre.api.model.TokenResetSenhaModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.TokenResetSenhaRepository;
import br.com.lumilivre.api.repository.UsuarioRepository;
import br.com.lumilivre.api.security.JwtUtil;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository ur;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenResetSenhaRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    public LoginResponse login(LoginRequest dto) {
        UsuarioModel usuario = ur.findByEmailOrAluno_Matricula(dto.getUser(), dto.getUser())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new BadCredentialsException("Senha incorreta");
        }

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));

        User userDetails = new User(
                usuario.getEmail(),
                usuario.getSenha(),
                authorities);

        String token = jwtUtil.generateToken(userDetails);

        return new LoginResponse(usuario, token);
    }

    @Transactional
    public void solicitarResetSenha(String email) {
        Optional<UsuarioModel> usuarioOpt = ur.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            UsuarioModel usuario = usuarioOpt.get();
            String token = UUID.randomUUID().toString();

            TokenResetSenhaModel tokenReset = new TokenResetSenhaModel(token, usuario, 30);
            tokenRepository.save(tokenReset);

            String linkReset = "https://www.lumilivre.com.br/mudar-senha?token=" + token;
            emailService.enviarEmailResetSenha(usuario.getEmail(), linkReset);
        }
    }

    public boolean validarTokenReset(String token) {
        Optional<TokenResetSenhaModel> tokenOpt = tokenRepository.findByToken(token);
        return tokenOpt.isPresent() && !tokenOpt.get().isExpirado();
    }

    @Transactional
    public void mudarSenhaComToken(MudarSenhaTokenRequest dto) {
        TokenResetSenhaModel tokenReset = tokenRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido ou não encontrado."));

        if (tokenReset.isExpirado()) {
            throw new IllegalArgumentException("Token expirado.");
        }

        UsuarioModel usuario = tokenReset.getUsuario();
        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        ur.save(usuario);

        tokenRepository.delete(tokenReset);
    }
}