package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.model.AutorModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AutorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/autores")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")

@Tag(name = "9. Autores")
@SecurityRequirement(name = "bearerAuth")

public class AutorController {

    @Autowired
    private AutorService as;
    
    public AutorController(AutorService AutorService) {
        this.as = AutorService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")

    @Operation(summary = "Busca autores com paginação e filtro de texto", description = "Retorna uma página de autores. Pode filtrar por um texto genérico que busca em nome, pseudônimo e nacionalidade.")
    @ApiResponse(responseCode = "200", description = "Página de autores retornada com sucesso")

    public ResponseEntity<Page<AutorModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AutorModel> autores = as.buscarPorTexto(texto, pageable);

        return autores.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(autores);
    } 

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")

    @Operation(summary = "Busca avançada e paginada de autores", description = "Filtra autores por campos específicos como nome, pseudônimo e nacionalidade.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Página de autores encontrada"),
        @ApiResponse(responseCode = "204", description = "Nenhum autor encontrado para os filtros")
    })

    public ResponseEntity<Page<AutorModel>> buscarAvancado(
            @Parameter(description = "Nome parcial do autor") @RequestParam(required = false) String nome,
            @Parameter(description = "Pseudônimo exato do autor") @RequestParam(required = false) String pseudonimo,
            @Parameter(description = "Nacionalidade do autor") @RequestParam(required = false) String nacionalidade,
            Pageable pageable) {
        Page<AutorModel> autores = as.buscarAvancado(nome, pseudonimo, nacionalidade, pageable);

        return autores.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(autores);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo autor", description = "Cria um novo autor no sistema.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "201", description = "Autor cadastrado com sucesso", content = @Content(schema = @Schema(implementation = AutorModel.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: código duplicado)")
    })

    public ResponseEntity<?> cadastrar(@RequestBody AutorModel am) {
        return as.cadastrar(am);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{codigo}")

    @Operation(summary = "Atualiza um autor existente", description = "Altera os dados de um autor com base no seu código.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Autor atualizado com sucesso", content = @Content(schema = @Schema(implementation = AutorModel.class))),
        @ApiResponse(responseCode = "404", description = "Autor não encontrado")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "Código do autor a ser atualizado") @PathVariable String codigo, 
            @RequestBody AutorModel am) {
        am.setCodigo(codigo);
        return as.atualizar(am);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{codigo}")

    @Operation(summary = "Exclui um autor", description = "Remove um autor do sistema.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Autor excluído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Autor não encontrado")
    })

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "Código do autor a ser excluído")  @PathVariable String codigo) {
        return as.excluir(codigo);
    }
}