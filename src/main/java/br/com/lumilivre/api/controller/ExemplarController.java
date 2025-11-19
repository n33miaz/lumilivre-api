package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.ExemplarDTO;
import br.com.lumilivre.api.dto.ListaLivroDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.ExemplarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @ApiResponse(responseCode = "200", description = "Lista de exemplares retornada com sucesso", content = @Content(schema = @Schema(implementation = ListaLivroDTO.class))),
            @ApiResponse(responseCode = "204", description = "Nenhum exemplar encontrado para este livro"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<List<ListaLivroDTO>> buscarPorLivroId(
            @Parameter(description = "ID do livro cujos exemplares serão listados") @PathVariable Long livroId) {

        List<ListaLivroDTO> exemplares = exemplarService.buscarExemplaresPorLivroId(livroId);

        if (exemplares.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(exemplares);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo exemplar")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exemplar cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<ResponseModel> cadastrar(@RequestBody ExemplarDTO exemplarDTO) {
        exemplarService.cadastrar(exemplarDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseModel("Exemplar cadastrado com sucesso."));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{tombo}")
    @Operation(summary = "Atualiza um exemplar existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exemplar atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Exemplar ou Livro não encontrado")
    })
    public ResponseEntity<ResponseModel> atualizar(
            @Parameter(description = "Código de tombo do exemplar a ser atualizado") @PathVariable String tombo,
            @RequestBody ExemplarDTO exemplarDTO) {

        exemplarService.atualizar(tombo, exemplarDTO);
        return ResponseEntity.ok(new ResponseModel("Exemplar atualizado com sucesso."));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{tombo}")
    @Operation(summary = "Exclui um exemplar")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exemplar excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir (está emprestado)"),
            @ApiResponse(responseCode = "404", description = "Exemplar não encontrado")
    })
    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "Código de tombo do exemplar a ser excluído") @PathVariable String tombo) {

        exemplarService.excluir(tombo);
        return ResponseEntity.ok(new ResponseModel("Exemplar excluído com sucesso."));
    }
}