package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.ExemplarService;

@RestController
@RequestMapping("/lumilivre/livros/exemplares")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class ExemplarController {
	
	@Autowired
	private ExemplarService exemplarService;
	
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody ExemplarDTO exemplarDTO) {
        return exemplarService.cadastrar(exemplarDTO);
    }
    
    @GetMapping("/listar")
    public Iterable<ExemplarModel> listar() {
        return exemplarService.listar();
    }
    
    @DeleteMapping("/remover/{tombo}")
    public ResponseEntity<ResponseModel> remover(@PathVariable String tombo) {
        return exemplarService.deletar(tombo);
    }
    
    @PutMapping("/alterar/{tombo}")
    public ResponseEntity<?> alterar(@PathVariable String tombo, @RequestBody ExemplarDTO exemplarDTO) {
        return exemplarService.alterar(exemplarDTO);
    }
}
