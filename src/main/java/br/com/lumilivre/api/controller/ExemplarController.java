package br.com.lumilivre.api.controller;

import java.util.List;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.livro.ExemplarRequest;
import br.com.lumilivre.api.dto.livro.LivroListagemResponse;
import br.com.lumilivre.api.service.ExemplarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livros/exemplares")
@Tag(name = "8. Exemplares")
@SecurityRequirement(name = "bearerAuth")
public class ExemplarController {

    private final ExemplarService exemplarService;

    public ExemplarController(ExemplarService es) {
        this.exemplarService = es;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/livro/{livroId}")
    @Operation(summary = "Busca todos os exemplares de um livro pelo ID do livro")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de exemplares retornada com sucesso", content = @Content(schema = @Schema(implementation = LivroListagemResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Nenhum exemplar encontrado para este livro"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<List<LivroListagemResponse>> buscarPorLivroId(
            @Parameter(description = "ID do livro cujos exemplares serão listados") @PathVariable Long livroId) {

        List<LivroListagemResponse> exemplares = exemplarService.buscarExemplaresPorLivroId(livroId);

        if (exemplares.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(exemplares);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo exemplar")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Exemplar cadastrado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> cadastrar(@RequestBody ExemplarRequest exemplarDTO) {
        exemplarService.cadastrar(exemplarDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Exemplar cadastrado com sucesso.", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{tombo}")
    @Operation(summary = "Atualiza um exemplar existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Exemplar atualizado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Exemplar ou Livro não encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> atualizar(
            @Parameter(description = "Código de tombo do exemplar a ser atualizado") @PathVariable String tombo,
            @RequestBody ExemplarRequest exemplarDTO) {

        exemplarService.atualizar(tombo, exemplarDTO);
        return ResponseEntity.ok(new ApiResponse<>(true, "Exemplar atualizado com sucesso.", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{tombo}")
    @Operation(summary = "Exclui um exemplar")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Exemplar excluído com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Não é possível excluir (está emprestado)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Exemplar não encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> excluir(
            @Parameter(description = "Código de tombo do exemplar a ser excluído") @PathVariable String tombo) {

        exemplarService.excluir(tombo);
        return ResponseEntity.ok(new ApiResponse<>(true, "Exemplar excluído com sucesso.", null));
    }
}