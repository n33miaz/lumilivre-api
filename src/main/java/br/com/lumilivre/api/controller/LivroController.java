package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.LivroDTO;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.LivroService;

@RestController
@RequestMapping("/livros")
@PreAuthorize("isAuthenticated()") 
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")
public class LivroController {

    @Autowired
    private LivroService ls;

    public LivroController(LivroService livroService) {
        this.ls = livroService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/buscar/todos")
    public Iterable<LivroModel> buscar() {
        return ls.buscar();
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/disponiveis")
    public ResponseEntity<Iterable<LivroModel>> listarDisponiveis() {
        Iterable<LivroModel> livrosDisponiveis = ls.buscarLivrosDisponiveis();
        if (!livrosDisponiveis.iterator().hasNext()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(livrosDisponiveis);
    }

    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/buscar")
    public ResponseEntity<Page<LivroModel>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<LivroModel> livros = ls.buscarPorTexto(texto, pageable);
        if (livros.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(livros);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/buscar/avancado")
    public ResponseEntity<Page<LivroModel>> buscarAvancado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String editora,
            Pageable pageable) {
        Page<LivroModel> livros = ls.buscarAvancado(nome, isbn, autor, genero, editora, pageable);
        if (livros.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody LivroDTO livroDTO) {
        return ls.cadastrar(livroDTO);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{isbn}")
    public ResponseEntity<?> atualizar(@PathVariable String isbn, @RequestBody LivroDTO livroDTO) {
        return ls.atualizar(livroDTO);
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{isbn}")
    public ResponseEntity<ResponseModel> excluir(@PathVariable String isbn) {
        return ls.excluir(isbn);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/excluir-com-exemplares/{isbn}")
    public ResponseEntity<?> excluirComExemplares(@PathVariable String isbn) {
        return ls.excluirLivroComExemplares(isbn);
    }

}
