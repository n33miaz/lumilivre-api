package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.AlunoDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AlunoService;

@RestController
@RequestMapping("/alunos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AlunoController {

    @Autowired
    private AlunoService as;

    @DeleteMapping("/remover/{id}")
    public ResponseEntity<ResponseModel> remover(@PathVariable String id) {
        return as.deletar(id);
    }

    @PutMapping("/alterar/{id}")
    public ResponseEntity<?> alterar(@PathVariable String id, @RequestBody AlunoDTO alunoDTO) {
        return as.alterar(id, alunoDTO);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody AlunoDTO alunoDTO) {
        return as.cadastrar(alunoDTO);
    }

    @GetMapping("/listar")
    public Iterable<AlunoModel> listar() {
        return as.listar();
    }
}
