package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AutorService;
import br.com.lumilivre.api.service.CursoService;

@Controller
@RequestMapping("/lumilivre/autores")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AutorController {

    @Autowired 
    private AutorService as;

    @DeleteMapping("remover/{codigo}")
    public ResponseEntity<ResponseModel> remover(@PathVariable String codigo) {
        return as.delete(codigo);
    }

    @PutMapping("alterar/{codigo}")
    public ResponseEntity<?> alterar(@PathVariable String codigo, @RequestBody AutorModel am) {
    	am.setCodigo(codigo);
        return as.alterar(am);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody AutorModel am) {
        return as.cadastrar(am);
    }

    @GetMapping("/listar")
    public List<AutorModel> listar() {
        return as.listar();
    }
}
