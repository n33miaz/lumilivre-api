package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AutorService;

@RestController
@RequestMapping("/lumilivre/autores")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class AutorController {

    @Autowired 
    private AutorService as;

    @DeleteMapping("/remover/{codigo}")
    public ResponseEntity<ResponseModel> remover(@PathVariable String codigo) {
        return as.delete(codigo);
    }

    @PutMapping("/alterar/{codigo}")
    public ResponseEntity<?> alterar(@PathVariable String codigo, @RequestBody AutorModel am) {
        am.setCodigo(codigo);
        return as.alterar(am);
    }


        @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody List<AutorModel> am) {
        return as.cadastrar(am);
    }

    @GetMapping("/listar")
    public Iterable<AutorModel> listar() {
        return as.listar();
    }
}
