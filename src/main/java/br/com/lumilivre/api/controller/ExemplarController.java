package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.ExemplarDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.ExemplarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/livros/exemplares")
@Tag(name = "8. Exemplares")
@SecurityRequirement(name = "bearerAuth")
public class ExemplarController {

    private final ExemplarService exemplarService;

    // Injeção de dependência via construtor
    public ExemplarController(ExemplarService es) {
        this.exemplarService = es;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/livro/{livroId}")
    @Operation(summary = "Busca todos os exemplares de um livro pelo ID do livro", description = "Retorna uma lista de todos os exemplares (cópias físicas) associados a um ID de livro específico.")
    @ApiResponse(responseCode = "200", description = "Lista de exemplares retornada com sucesso")
    public ResponseEntity<?> buscarPorLivroId(
            @Parameter(description = "ID do livro cujos exemplares serão listados") @PathVariable Long livroId) {
        return exemplarService.buscarExemplaresPorLivroId(livroId);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo exemplar", description = "Cria um novo exemplar (cópia física) para um livro já existente, identificado pelo ID do livro.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exemplar cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: ID do livro não encontrado, Tombo duplicado)")
    })
    public ResponseEntity<ResponseModel> cadastrar(@RequestBody ExemplarDTO exemplarDTO) {
        return exemplarService.cadastrar(exemplarDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{tombo}")
    @Operation(summary = "Atualiza um exemplar existente", description = "Altera os dados de um exemplar específico, identificado pelo seu código de tombo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exemplar atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou exemplar não encontrado")
    })
    public ResponseEntity<ResponseModel> atualizar(
            @Parameter(description = "Código de tombo do exemplar a ser atualizado") @PathVariable String tombo,
            @RequestBody ExemplarDTO exemplarDTO) {
        // A chamada agora passa o tombo e o DTO separadamente para o serviço
        return exemplarService.atualizar(tombo, exemplarDTO);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{tombo}")
    @Operation(summary = "Exclui um exemplar", description = "Remove um exemplar específico do sistema, identificado pelo seu código de tombo. A exclusão falhará se o exemplar estiver em um empréstimo ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exemplar excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir, exemplar está emprestado"),
            @ApiResponse(responseCode = "404", description = "Exemplar não encontrado")
    })
    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "Código de tombo do exemplar a ser excluído") @PathVariable String tombo) {
        return exemplarService.excluir(tombo);
    }
}