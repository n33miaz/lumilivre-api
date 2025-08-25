package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.ExemplarService;

@RestController
@PreAuthorize("isAuthenticated()") 
@RequestMapping("/livros/exemplares")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class ExemplarController {

    @Autowired
    private ExemplarService es;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarTodos() {
        List<ExemplarModel> lista = es.buscar();

        List<ExemplarDTO> dtos = lista.stream().map(exemplar -> {
            ExemplarDTO dto = new ExemplarDTO();
            dto.setTombo(exemplar.getTombo());
            dto.setStatus_livro(exemplar.getStatus_livro().toString());
            dto.setLivro_isbn(exemplar.getLivro_isbn().getIsbn());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

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
