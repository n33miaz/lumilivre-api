package br.com.lumilivre.api.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.data.AlterarSenhaDTO;
import br.com.lumilivre.api.data.ListaUsuarioDTO;
import br.com.lumilivre.api.data.UsuarioDTO;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UsuarioService {

    private final EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository ur;

    UsuarioService(EmailService emailService) {
        this.emailService = emailService;
    }
    
    
    public Page<ListaUsuarioDTO> buscarUsuarioParaListaAdmin(Pageable pageable) {
        return ur.findUsuarioParaListaAdmin(pageable);
    }

    public Page<UsuarioModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return ur.findAll(pageable);
        }
        return ur.buscarPorTexto(texto, pageable);
    }
    public Page<UsuarioModel> buscarAvancado(
            Integer id,
            String email,
            Role role,

            Pageable pageable) {
        return ur.buscarAvancado(id, email, role, pageable);
    }

    @Transactional
    public ResponseEntity<?> cadastrarAdmin(UsuarioDTO dto) {
        ResponseModel rm = new ResponseModel();

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            rm.setMensagem("O e-mail é obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }

        if (ur.existsByEmail(dto.getEmail())) {
            rm.setMensagem("E-mail já está em uso");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(rm);
        }

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            rm.setMensagem("A senha é obrigatória");
            return ResponseEntity.badRequest().body(rm);
        }

        UsuarioModel usuarioModel = new UsuarioModel();
        usuarioModel.setEmail(dto.getEmail());
        usuarioModel.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuarioModel.setRole(Role.ADMIN);

        UsuarioModel salvo = ur.save(usuarioModel);

        emailService.enviarSenhaInicial(dto.getEmail(), "Admin", dto.getSenha());

        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @Transactional
    public ResponseEntity<?> atualizar(Integer id, UsuarioDTO dto) {
        ResponseModel rm = new ResponseModel();
        Optional<UsuarioModel> optionalUsuario = ur.findById(id);

        if (optionalUsuario.isEmpty()) {
            rm.setMensagem("Usuário não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        UsuarioModel usuarioModel = optionalUsuario.get();

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            rm.setMensagem("O e-mail é obrigatório");
            return ResponseEntity.badRequest().body(rm);
        }
        if (!dto.getEmail().equals(usuarioModel.getEmail()) && ur.existsByEmail(dto.getEmail())) {
            rm.setMensagem("E-mail já está em uso");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(rm);
        }
        usuarioModel.setEmail(dto.getEmail());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuarioModel.setSenha(passwordEncoder.encode(dto.getSenha()));

            emailService.enviarSenhaInicial(dto.getEmail(), "Admin", dto.getSenha());
        }

        UsuarioModel salvo = ur.save(usuarioModel);
        return ResponseEntity.ok(salvo);
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        Optional<UsuarioModel> optUsuario = ur.findById(id);
        if (optUsuario.isEmpty()) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("Usuário não encontrado.");
            return new ResponseEntity<>(rm, HttpStatus.NOT_FOUND);
        }

        UsuarioModel usuario = optUsuario.get();

        if (usuario.getRole() == Role.ALUNO && usuario.getAluno() != null) {
            usuario.getAluno().setUsuario(null);
        }

        ur.delete(usuario);

        ResponseModel rm = new ResponseModel();
        rm.setMensagem("Usuário removido com sucesso");
        return new ResponseEntity<>(rm, HttpStatus.OK);
    }

    public ResponseEntity<?> alterarSenha(AlterarSenhaDTO dto) {
        Optional<UsuarioModel> opt = ur.findByEmail(dto.getMatricula());

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado");
        }

        UsuarioModel usuario = opt.get();

        if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        ur.save(usuario);

        return ResponseEntity.ok("Senha alterada com sucesso");
    }

}
