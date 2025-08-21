package br.com.lumilivre.api.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.lumilivre.api.data.LoginDTO;
import br.com.lumilivre.api.data.LoginResponseDTO;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.UsuarioRepository;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository ur;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<?> login(LoginDTO dto) {
        Optional<UsuarioModel> opt = ur.findByEmailOrAluno_Matricula(dto.getUser(), dto.getUser());

        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body("Usuário não encontrado");
        }

        UsuarioModel usuario = opt.get();

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            return ResponseEntity.status(401).body("Senha incorreta");
        }

        return ResponseEntity.ok(new LoginResponseDTO(usuario));
    }
}
