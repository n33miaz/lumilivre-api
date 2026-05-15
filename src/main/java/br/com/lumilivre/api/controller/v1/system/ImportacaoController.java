package br.com.lumilivre.api.controller.v1.system;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.lumilivre.api.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/importacao")
@Tag(name = "15. Importação")
@SecurityRequirement(name = "bearerAuth")
public class ImportacaoController {

    private final ImportService importService;

    public ImportacaoController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(value = "/alunos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa alunos via Excel (.xlsx)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Importação processada (retorna resumo)"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou formato incorreto"),
            @ApiResponse(responseCode = "500", description = "Erro interno durante o processamento")
    })
    public ResponseEntity<String> importarAlunos(
            @Parameter(description = "Arquivo Excel (.xlsx) contendo os dados dos alunos") 
            @RequestParam("file") MultipartFile file) {
        return processarImportacao("aluno", file);
    }

    @PostMapping(value = "/livros", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa livros via Excel (.xlsx)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Importação processada (retorna resumo)"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou formato incorreto"),
            @ApiResponse(responseCode = "500", description = "Erro interno durante o processamento")
    })
    public ResponseEntity<String> importarLivros(
            @Parameter(description = "Arquivo Excel (.xlsx) contendo os dados dos livros") 
            @RequestParam("file") MultipartFile file) {
        return processarImportacao("livro", file);
    }

    @PostMapping(value = "/exemplares", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Importa exemplares via Excel (.xlsx)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Importação processada (retorna resumo)"),
            @ApiResponse(responseCode = "400", description = "Arquivo inválido ou formato incorreto"),
            @ApiResponse(responseCode = "500", description = "Erro interno durante o processamento")
    })
    public ResponseEntity<String> importarExemplares(
            @Parameter(description = "Arquivo Excel (.xlsx) contendo os dados dos exemplares") 
            @RequestParam("file") MultipartFile file) {
        return processarImportacao("exemplar", file);
    }

    private ResponseEntity<String> processarImportacao(String tipo, MultipartFile file) {
        try {
            String mensagem = importService.importar(tipo, file);
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
