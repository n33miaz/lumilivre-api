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
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.GeneroService;

@RestController
@RequestMapping("/livros/generos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")

public class GeneroController {
	@Autowired
	private GeneroService gs;
	
    @DeleteMapping("remover/{id}")
    public ResponseEntity<ResponseModel> remover(@PathVariable Integer id) {
        return gs.delete(id);
    }

    @PutMapping("alterar/{id}")
    public ResponseEntity<?> alterar(@PathVariable Integer id, @RequestBody GeneroModel gm) {
    	gm.setId(id);
        return gs.alterar(gm);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody GeneroModel gm) {
        return gs.cadastrar(gm);
    }

    @GetMapping("/listar")
    public List<GeneroModel> listar() {
        return gs.listar();
    }
}
