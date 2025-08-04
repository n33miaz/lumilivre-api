package br.com.lumilivre.api.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.EmprestimoService;
@RestController
@RequestMapping("/emprestimos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class EmprestimoController {

    @Autowired
    private EmprestimoService es;

    @DeleteMapping("/remover/{id}")
    public ResponseEntity<ResponseModel> remover(@PathVariable Integer id) {
        return es.delete(id);
    }

    @PutMapping("/alterar/{id}")
    public ResponseEntity<?> alterar(@PathVariable Integer id, @RequestBody EmprestimoDTO dto) {
        dto.setId(id);
        return es.alterar(dto);
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody EmprestimoDTO dto) {
        return es.cadastrar(dto);
    }

    @GetMapping("/listar")
    public Iterable<EmprestimoModel> listar() {
        return es.listar();
    }
    
    @GetMapping("/ativos")
    public ResponseEntity<List<EmprestimoModel>> listarAtivos() {
        return ResponseEntity.ok(es.listarAtivos());
    }

    @GetMapping("/atrasados")
    public ResponseEntity<List<EmprestimoModel>> listarAtrasados() {
        return ResponseEntity.ok(es.listarAtrasados());
    } 

    @GetMapping("/concluidos")
    public ResponseEntity<List<EmprestimoModel>> listarConcluidos() {
        return ResponseEntity.ok(es.listarConcluidos());
    }

}
