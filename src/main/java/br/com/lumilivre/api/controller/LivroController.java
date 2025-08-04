package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.LivroDTO;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.LivroService;

@RestController
@RequestMapping("/livros")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class LivroController {

    @Autowired
    private LivroService ls;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody LivroDTO livroDTO) {
        return ls.cadastrar(livroDTO);
    }

    @GetMapping("/listar")
    public Iterable<LivroModel> listar() {
        return ls.listar();
    }

    @DeleteMapping("/remover/{isbn}")
    public ResponseEntity<ResponseModel> remover(@PathVariable String isbn) {
        return ls.deletar(isbn);
    }

    @DeleteMapping("/remover-com-exemplares/{isbn}")
    public ResponseEntity<?> removerComExemplares(@PathVariable String isbn) {
        return ls.excluirLivroComExemplares(isbn);
    }

    @PutMapping("/alterar/{isbn}")
    public ResponseEntity<?> alterar(@PathVariable String isbn, @RequestBody LivroDTO livroDTO) {
        return ls.alterar(livroDTO);
    }
}
