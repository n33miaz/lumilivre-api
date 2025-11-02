package br.com.lumilivre.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.service.ImportacaoLegadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/importacao/legado")
@Tag(name = "11. Importação de Dados Legados")
@SecurityRequirement(name = "bearerAuth")
public class ImportacaoLegadoController {

    private final ImportacaoLegadoService importacaoLegadoService;

    public ImportacaoLegadoController(ImportacaoLegadoService importacaoLegadoService) {
        this.importacaoLegadoService = importacaoLegadoService;
    }

    @PostMapping("/livros")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa uma planilha de livros do sistema legado")
    public ResponseEntity<String> importarLivrosLegado(@RequestParam("file") MultipartFile file) {
        try {
            String mensagem = importacaoLegadoService.importarLivros(file);
            return ResponseEntity.ok(mensagem);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erro ao importar livros legados: " + e.getMessage());
        }
    }

    @PostMapping("/exemplares")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa uma planilha de exemplares do sistema legado")
    public ResponseEntity<String> importarExemplaresLegado(@RequestParam("file") MultipartFile file) {
        try {
            String mensagem = importacaoLegadoService.importarExemplares(file);
            return ResponseEntity.ok(mensagem);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Erro ao importar exemplares legados: " + e.getMessage());
        }
    }
}