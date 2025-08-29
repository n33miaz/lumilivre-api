package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.GeneroService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.GeneroService;

@RestController
@RequestMapping("/livros/generos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")

@Tag(name = "9. Gêneros")
@SecurityRequirement(name = "bearerAuth")

public class GeneroController {
    @Autowired
    private GeneroService gs;
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    public ResponseEntity<Page<ListaGeneroDTO>> buscarGenerosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {

        Page<ListaGeneroDTO> generos = gs.buscarGeneroParaListaAdmin(pageable);

        if (generos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(generos);
    }


    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    public ResponseEntity<Page<GeneroModel>> buscarPorTexto(
            @RequestParam(required = false) String texto, 
            Pageable pageable) {
        Page<GeneroModel> generos = gs.buscarPorTexto(texto, pageable);
        if (generos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(generos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    public ResponseEntity<Page<GeneroModel>> buscarAvancado(
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<GeneroModel> generos = gs.buscarAvancado(id, nome, pageable);
        if (generos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(generos);
    }

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
