package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.data.ListaGeneroDTO;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.GeneroService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/livros/generos")

@Tag(name = "10. Gêneros")
@SecurityRequirement(name = "bearerAuth")

public class GeneroController {
    @Autowired
    private GeneroService gs;
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")

    @Operation(summary = "Lista gêneros para a tela principal do admin", description = "Retorna uma lista paginada de gêneros com dados resumidos para a exibição no dashboard. Suporta filtro de texto.")
    @ApiResponse(responseCode = "200", description = "Página de gêneros retornada com sucesso")

    public ResponseEntity<Page<ListaGeneroDTO>> buscarGenerosAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaGeneroDTO> generos = gs.buscarGeneroParaListaAdmin(pageable);

        return generos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(generos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")

    @Operation(summary = "Busca gêneros com paginação e filtro de texto", description = "Retorna uma página de gêneros. Pode filtrar por um texto genérico.")
    @ApiResponse(responseCode = "200", description = "Página de gêneros retornada com sucesso")

    public ResponseEntity<Page<GeneroModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica no nome do gênero") @RequestParam(required = false) String texto, 
            Pageable pageable) {
        Page<GeneroModel> generos = gs.buscarPorTexto(texto, pageable);

        return generos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(generos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")

    @Operation(summary = "Busca avançada e paginada de gêneros", description = "Filtra gêneros por ID ou nome.")
    @ApiResponse(responseCode = "200", description = "Página de gêneros retornada com sucesso")

    public ResponseEntity<Page<GeneroModel>> buscarAvancado(
            @Parameter(description = "ID exato do gênero") @RequestParam(required = false) Integer id,
            @Parameter(description = "Nome parcial do gênero") @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<GeneroModel> generos = gs.buscarAvancado(id, nome, pageable);

        return generos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(generos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo gênero", description = "Cria um novo gênero literário no sistema.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "201", description = "Gênero cadastrado com sucesso", content = @Content(schema = @Schema(implementation = GeneroModel.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: nome em branco ou duplicado)")
    })

    public ResponseEntity<?> cadastrar(@RequestBody GeneroModel gm) {
        return gs.cadastrar(gm);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("atualizar/{id}")

    @Operation(summary = "Atualiza um gênero existente", description = "Altera o nome de um gênero com base no seu ID.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Gênero atualizado com sucesso", content = @Content(schema = @Schema(implementation = GeneroModel.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: nome duplicado)"),
        @ApiResponse(responseCode = "404", description = "Gênero não encontrado para o ID fornecido")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "ID do gênero a ser atualizado") @PathVariable Integer id, 
            @RequestBody GeneroModel gm) {
        gm.setId(id);
        return gs.atualizar(gm);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("excluir/{id}")

    @Operation(summary = "Exclui um gênero", description = "Remove um gênero do sistema.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Gênero excluído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Gênero não encontrado para o ID fornecido")
    })

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "ID do gênero a ser excluído") @PathVariable Integer id) {
        return gs.excluir(id);
    }
}