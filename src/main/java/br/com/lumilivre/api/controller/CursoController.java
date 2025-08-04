package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.CursoService;

@RestController
@RequestMapping("/cursos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class CursoController {

    @Autowired 
    private CursoService cs;

    @DeleteMapping("remover/{id}")
    public ResponseEntity<ResponseModel> remover(@PathVariable Integer id) {
        return cs.delete(id);
    }

    @PutMapping("alterar/{id}")
    public ResponseEntity<?> alterar(@PathVariable Integer id, @RequestBody CursoModel cm) {
        cm.setId(id);
        return cs.alterar(cm);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody CursoModel cm) {
        return cs.cadastrar(cm);
    }

    @GetMapping("/listar")
    public List<CursoModel> listar() {
        return cs.listar();
    }
}
