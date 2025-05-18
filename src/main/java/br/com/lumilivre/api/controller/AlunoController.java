package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.AlunoDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AlunoService;

@Controller
@RequestMapping("/lumilivre/alunos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AlunoController {

    private static final AlunoDTO AlunoModel = null;
	@Autowired
    private AlunoService as;

    @DeleteMapping("remover/{id}")
    public ResponseEntity<ResponseModel> remover(@PathVariable String matricula) {
        return as.deletar(matricula);
    }

    @PutMapping("alterar/{id}")
    public ResponseEntity<?> alterar(@PathVariable String matricula, @RequestBody AlunoModel alunoModel) {
        alunoModel.setMatricula(matricula);
        return as.alterar(matricula, AlunoModel);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody AlunoDTO alunoDTO) {
        return as.cadastrar(alunoDTO);
    }

    @GetMapping("/listar")
    public List<AlunoModel> listar() {
        return as.listar();
    }
}