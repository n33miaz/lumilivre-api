package br.com.lumilivre.api.legado;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/importacao/legado/alunos")
@Tag(name = "11. Importação de Dados Legados")
@SecurityRequirement(name = "bearerAuth")
public class ImportacaoLegadoControllerAluno {

    private final ImportacaoLegadoServiceAluno importacaoService;

    public ImportacaoLegadoControllerAluno(ImportacaoLegadoServiceAluno importacaoService) {
        this.importacaoService = importacaoService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa uma planilha de alunos e cria seus usuários")
    public ResponseEntity<String> importarAlunos(@RequestParam("file") MultipartFile file) {
        try {
            String mensagem = importacaoService.importarAlunos(file);
            return ResponseEntity.ok(mensagem);
        } catch (Exception e) {
            e.printStackTrace(); // Importante para debug no console
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao importar alunos: " + e.getMessage());
        }
    }
}