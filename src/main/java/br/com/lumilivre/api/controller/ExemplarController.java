package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.ExemplarDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.ExemplarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/livros/exemplares")

@Tag(name = "8. Exemplares")
@SecurityRequirement(name = "bearerAuth")

public class ExemplarController {

    @Autowired
    private ExemplarService es;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/{isbn}")

    @Operation(summary = "Busca todos os exemplares de um livro", description = "Retorna uma lista de todos os exemplares (cópias físicas) associados a um ISBN específico.")
    @ApiResponse(responseCode = "200", description = "Lista de exemplares retornada com sucesso")

    public ResponseEntity<?> buscarPorIsbn(
            @Parameter(description = "ISBN do livro cujos exemplares serão listados") @PathVariable String isbn) {
        return es.buscarExemplaresPorIsbn(isbn);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo exemplar", description = "Cria um novo exemplar (cópia física) para um livro já existente, identificado pelo ISBN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exemplar cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: ISBN não encontrado, Tombo duplicado)")
    })

    public ResponseEntity<?> cadastrar(@RequestBody ExemplarDTO exemplarDTO) {
        return es.cadastrar(exemplarDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{tombo}")

    @Operation(summary = "Atualiza um exemplar existente", description = "Altera os dados de um exemplar específico, identificado pelo seu código de tombo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exemplar atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou exemplar não encontrado")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "Código de tombo do exemplar a ser atualizado") @PathVariable String tombo,
            @RequestBody ExemplarDTO exemplarDTO) {
        exemplarDTO.setTombo(tombo);
        return es.atualizar(exemplarDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{tombo}")

    @Operation(summary = "Exclui um exemplar", description = "Remove um exemplar específico do sistema, identificado pelo seu código de tombo.")
    @ApiResponse(responseCode = "200", description = "Exemplar excluído com sucesso")

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "Código de tombo do exemplar a ser excluído") @PathVariable String tombo) {
        return es.excluir(tombo);
    }
}