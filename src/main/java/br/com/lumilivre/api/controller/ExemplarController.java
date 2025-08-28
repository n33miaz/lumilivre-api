package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.ExemplarService;

@RestController
@RequestMapping("/livros/exemplares")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class ExemplarController {

    @Autowired
    private ExemplarService es;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/{isbn}")
    public ResponseEntity<?> buscarPorIsbn(@PathVariable String isbn) {
        return es.buscarExemplaresPorIsbn(isbn);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody ExemplarDTO exemplarDTO) {
        return es.cadastrar(exemplarDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{tombo}")
    public ResponseEntity<?> atualizar(@PathVariable String tombo, @RequestBody ExemplarDTO exemplarDTO) {
        exemplarDTO.setTombo(tombo);
        return es.atualizar(exemplarDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{tombo}")
    public ResponseEntity<ResponseModel> excluir(@PathVariable String tombo) {
        return es.excluir(tombo);
    }

}
