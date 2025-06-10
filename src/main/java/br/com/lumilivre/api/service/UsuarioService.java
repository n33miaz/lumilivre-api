package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.UsuarioRepository;
import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository ur;

    // @Autowired
    // private PasswordEncoder passwordEncoder;


    @Transactional
    public ResponseEntity<?> cadastrar(UsuarioModel usuarioModel) {
        if (isEmailInvalido(usuarioModel)) {
            ResponseModel rm = new ResponseModel();
            rm.setMensagem("O email é obrigátorio");
            return ResponseEntity.badRequest().body(rm);
        }
        UsuarioModel salvo = ur.save(usuarioModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }
    
    private boolean isEmailInvalido(UsuarioModel usuarioModel) {
        return usuarioModel.getEmail() == null || usuarioModel.getEmail().trim().isEmpty();
    }

    // CRIAR VALIDAÇÃO DO EMAIL - REGEX
    // CRIAR VALIDAÇÃO DE SENHA
    // CRIAR VALIDAÇÃO DO ROLE
}





