package br.com.lumilivre.api.controller.system;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.lumilivre.api.service.ImportacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/importacao")
@Tag(name = "99. Importação", description = "Endpoints para importação em massa via Excel")
public class ImportacaoController {

    private final ImportacaoService importacaoService;

    public ImportacaoController(ImportacaoService importacaoService) {
        this.importacaoService = importacaoService;
    }

    @PostMapping("/alunos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa alunos via Excel (.xlsx)")
    public ResponseEntity<String> importarAlunos(@RequestParam("file") MultipartFile file) {
        return processarImportacao("aluno", file);
    }

    @PostMapping("/livros")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa livros via Excel (.xlsx)")
    public ResponseEntity<String> importarLivros(@RequestParam("file") MultipartFile file) {
        return processarImportacao("livro", file);
    }

    @PostMapping("/exemplares")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa exemplares via Excel (.xlsx)")
    public ResponseEntity<String> importarExemplares(@RequestParam("file") MultipartFile file) {
        return processarImportacao("exemplar", file);
    }

    private ResponseEntity<String> processarImportacao(String tipo, MultipartFile file) {
        try {
            String mensagem = importacaoService.importar(tipo, file);
            return ResponseEntity.ok(mensagem);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro crítico na importação: " + e.getMessage());
        }
    }
}