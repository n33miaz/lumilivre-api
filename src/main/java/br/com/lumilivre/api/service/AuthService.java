package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.LoginDTO;
import br.com.lumilivre.api.data.LoginResponseDTO;
import br.com.lumilivre.api.model.UsuarioModel;
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

    public ResponseEntity<?> login(LoginDTO dto) {
        Optional<UsuarioModel> opt = ur.findByEmailOrAluno_Matricula(dto.getUser(), dto.getUser());

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuário não encontrado");
        }

        UsuarioModel usuario = opt.get();

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            return ResponseEntity.status(401).body("Senha incorreta");
        }

        // Cria UserDetails com a role do usuário
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())
        );

        User userDetails = new User(
                usuario.getEmail(),
                usuario.getSenha(),
                authorities
        );

        // Gera o token JWT
        String token = jwtUtil.generateToken(userDetails);

        // Retorna o DTO com token
        return ResponseEntity.ok(new LoginResponseDTO(usuario, token));
    }
}
