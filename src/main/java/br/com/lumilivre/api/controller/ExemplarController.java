package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.ExemplarService;

@RestController
@RequestMapping("/livros/exemplares")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class ExemplarController {

    @Autowired
    private ExemplarService es;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody ExemplarDTO exemplarDTO) {
        return es.cadastrar(exemplarDTO);
    }

   @GetMapping("/listar")
    public ResponseEntity<?> listarTodos() {
        List<ExemplarModel> lista = es.listar();

        List<ExemplarDTO> dtos = lista.stream().map(exemplar -> {
            ExemplarDTO dto = new ExemplarDTO();
            dto.setTombo(exemplar.getTombo());
            dto.setStatus_livro(exemplar.getStatus_livro().toString());
            dto.setLivro_isbn(exemplar.getLivro_isbn().getIsbn());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/listar/{isbn}")
    public ResponseEntity<?> listarPorIsbn(@PathVariable String isbn) {
        return es.buscarExemplaresPorIsbn(isbn);
    }

    @DeleteMapping("/remover/{tombo}")
    public ResponseEntity<ResponseModel> remover(@PathVariable String tombo) {
        return es.deletar(tombo);
    }

    @PutMapping("/alterar/{tombo}")
    public ResponseEntity<?> alterar(@PathVariable String tombo, @RequestBody ExemplarDTO exemplarDTO) {
        exemplarDTO.setTombo(tombo);
        return es.alterar(exemplarDTO);
    }
} 
