package br.com.lumilivre.api.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.UsuarioDTO;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
@Service
public class UsuarioService {

	
	@Autowired
	private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository ur;

    @Transactional
    public ResponseEntity<?> cadastrarAdmin(UsuarioDTO dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O e-mail é obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("A senha é obrigatória");
            return ResponseEntity.badRequest().body(rm);
        }

        if (ur.existsByEmail(dto.getEmail())) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("E-mail já está em uso");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(rm);
        }

        UsuarioModel usuarioModel = new UsuarioModel();
        usuarioModel.setEmail(dto.getEmail());
        usuarioModel.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuarioModel.setRole(Role.ADMIN);

        UsuarioModel salvo = ur.save(usuarioModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
    
    @Transactional
    public ResponseEntity<?> alterar(Integer id, UsuarioDTO dto) {
        Optional<UsuarioModel> optionalUsuario = ur.findById(id);
        if (optionalUsuario.isEmpty()) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("Usuário não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        UsuarioModel usuarioModel = optionalUsuario.get();

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O e-mail é obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        if (!dto.getEmail().equals(usuarioModel.getEmail()) && ur.existsByEmail(dto.getEmail())) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("E-mail já está em uso");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(rm);
        }
        usuarioModel.setEmail(dto.getEmail());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuarioModel.setSenha(passwordEncoder.encode(dto.getSenha()));
        }


        UsuarioModel salvo = ur.save(usuarioModel);
        return ResponseEntity.ok(salvo);
    }


    
    @Transactional
    public ResponseEntity<ResponseModel> delete(Integer id) {
        ur.deleteById(id);
        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O ADM foi removido com sucesso");
        return new ResponseEntity<>(rm, HttpStatus.OK);
    }
    
    public Iterable<UsuarioModel> listar() {
        return ur.findAll();
    }

}

