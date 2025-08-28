package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.data.AlterarSenhaDTO;
import br.com.lumilivre.api.data.UsuarioDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService us;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody @Valid UsuarioDTO dto) {
        return us.cadastrarAdmin(dto);
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Integer id, @RequestBody @Valid UsuarioDTO dto) {
        return us.atualizar(id, dto);
    }

    @DeleteMapping("/excluir/{id}")
    public ResponseEntity<ResponseModel> excluir(@PathVariable Integer id) {
        return us.excluir(id);
    }

    @PutMapping("/alterar-senha")
    public ResponseEntity<?> alterarSenha(@RequestBody AlterarSenhaDTO dto) {
        return us.alterarSenha(dto);
    }

}
