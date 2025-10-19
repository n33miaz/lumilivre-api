package br.com.lumilivre.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.service.ImportacaoService;

@RestController
@RequestMapping("/importacao")
public class ImportacaoController {

    private final ImportacaoService importacaoService;

    public ImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    // 📘 Alunos
    @PostMapping("/alunos")
    public ResponseEntity<String> importarAlunos(@RequestParam("file") MultipartFile file) {
        try {
            String mensagem = importacaoService.importar("aluno", file);
            return ResponseEntity.ok(mensagem);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erro ao importar alunos: " + e.getMessage());
        }
    }

    // 📚 Livros
    @PostMapping("/livros")
    public ResponseEntity<String> importarLivros(@RequestParam("file") MultipartFile file) {
        try {
            String mensagem = importacaoService.importar("livro", file);
            return ResponseEntity.ok(mensagem);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erro ao importar livros: " + e.getMessage());
        }
    }

    // 🏷️ Exemplares
    @PostMapping("/exemplares")
    public ResponseEntity<String> importarExemplares(@RequestParam("file") MultipartFile file) {
        try {
            String mensagem = importacaoService.importar("exemplar", file);
            return ResponseEntity.ok(mensagem);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erro ao importar exemplares: " + e.getMessage());
        }
    }
}
