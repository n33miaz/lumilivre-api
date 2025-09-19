package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.data.LoginDTO;
import br.com.lumilivre.api.data.LoginResponseDTO;
import br.com.lumilivre.api.data.MudarSenhaComTokenDTO;
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

    public ResponseEntity<?> login(LoginDTO dto) {
        Optional<UsuarioModel> opt = ur.findByEmailOrAluno_Matricula(dto.getUser(), dto.getUser());

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuário não encontrado");
        }

        UsuarioModel usuario = opt.get();

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            return ResponseEntity.status(401).body("Senha incorreta");
        }

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()));

        User userDetails = new User(
                usuario.getEmail(),
                usuario.getSenha(),
                authorities);

        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new LoginResponseDTO(usuario, token));
    }

    // solicitar, validar e executar o reset de senha
    @Transactional
    public void solicitarResetSenha(String email) {
        Optional<UsuarioModel> usuarioOpt = ur.findByEmail(email);

        // se o usuário não existe, nada é feito
        if (usuarioOpt.isPresent()) {
            UsuarioModel usuario = usuarioOpt.get();
            
            // token aleatório
            String token = UUID.randomUUID().toString();
            
            // salva o token, associado ao usuário
            TokenResetSenhaModel tokenReset = new TokenResetSenhaModel(token, usuario, 30);
            tokenRepository.save(tokenReset);

            // alteraremos para o link do domínio + /mudar-senha?token=
            String linkReset = "https://lumilivre-web.onrender.com/mudar-senha?token=" + token;
            emailService.enviarEmailResetSenha(usuario.getEmail(), linkReset);
        }
    }

    public boolean validarTokenReset(String token) {
        Optional<TokenResetSenhaModel> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpirado()) {
            return false; // Token não encontrado ou expirado
        }
        return true;
    }

    @Transactional
    public void mudarSenhaComToken(MudarSenhaComTokenDTO dto) {
        Optional<TokenResetSenhaModel> tokenOpt = tokenRepository.findByToken(dto.getToken());

        if (tokenOpt.isEmpty() || tokenOpt.get().isExpirado()) {
            throw new IllegalArgumentException("Token inválido ou expirado.");
        }

        TokenResetSenhaModel tokenReset = tokenOpt.get();
        UsuarioModel usuario = tokenReset.getUsuario();
        
        // altera a senha do usuário
        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        ur.save(usuario);

        // invalida o token
        tokenRepository.delete(tokenReset);
    }
}
