package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.service.AlunoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/lumilivre/alunos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")

public class AlunoController {

    @Autowired
    private AlunoService as;

    @GetMapping("/listar")
    public Iterable<AlunoModel> listar() {
        return as.listar();
    }
    
    @DeleteMapping("/remover/{rm}")
    public ResponseEntity<?> remover (@PathVariable String matricula){
        return as.delete(matricula);
    }

    @PutMapping("alterar/{rm}")
    public ResponseEntity<?> alterar (@PathVariable String rm, @RequestBody AlunoModel am){
        am.setRm(rm);
        return as.cadastrarAlterar(am, "alterar");
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar (@RequestBody AlunoModel am){
        return as.cadastrarAlterar(am, "cadastrar");
    }
    
}

