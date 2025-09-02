package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.ListaCursoDTO;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.CursoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/cursos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")

@Tag(name = "10. Cursos")
@SecurityRequirement(name = "bearerAuth")

public class CursoController {

    @Autowired
    private CursoService cs;

    public CursoController(CursoService CursoService) {
        this.cs = CursoService;
    }
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")

    @Operation(summary = "Lista cursos para a tela principal do admin", description = "Retorna uma lista paginada de cursos com dados resumidos para a exibição no dashboard. Suporta filtro de texto. Acesso: ADMIN, BIBLIOTECARIO.")
    @ApiResponse(responseCode = "200", description = "Página de cursos retornada com sucesso")

    public ResponseEntity<Page<ListaCursoDTO>> buscarCursosAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaCursoDTO> cursos = cs.buscarCursoParaListaAdmin(pageable);

        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    } 

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")

    @Operation(summary = "Busca cursos com paginação e filtro de texto", description = "Retorna uma página de cursos. Pode filtrar por um texto genérico.")
    @ApiResponse(responseCode = "200", description = "Página de cursos retornada com sucesso")

    public ResponseEntity<Page<CursoModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<CursoModel> cursos = cs.buscarPorTexto(texto, pageable);

        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")

    @Operation(summary = "Busca avançada e paginada de cursos", description = "Filtra cursos por campos específicos como nome, turno e módulo.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Página de cursos encontrada"),
        @ApiResponse(responseCode = "204", description = "Nenhum curso encontrado para os filtros")
    })

    public ResponseEntity<Page<CursoModel>> buscarAvancado(
            @Parameter(description = "Nome parcial do curso") @RequestParam(required = false) String nome,
            @Parameter(description = "Turno do curso (MANHA, TARDE, NOITE, INTEGRAL)") @RequestParam(required = false) String turno,
            @Parameter(description = "Módulo ou série do curso") @RequestParam(required = false) String modulo,
            Pageable pageable) {
        Page<CursoModel> cursos = cs.buscarAvancado(nome, turno, modulo, pageable);

        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo curso", description = "Cria um novo curso no sistema.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "201", description = "Curso cadastrado com sucesso", content = @Content(schema = @Schema(implementation = CursoModel.class))),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (ex: nome em branco)")
    })
    
    public ResponseEntity<?> cadastrar(@RequestBody CursoModel cm) {
        return cs.cadastrar(cm);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("atualizar/{id}")

    @Operation(summary = "Atualiza um curso existente", description = "Altera os dados de um curso com base no seu ID.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Curso atualizado com sucesso", content = @Content(schema = @Schema(implementation = CursoModel.class))),
        @ApiResponse(responseCode = "404", description = "Curso não encontrado para o ID fornecido")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "ID do curso a ser atualizado") @PathVariable Integer id, 
            @RequestBody CursoModel cm) {
        cm.setId(id);
        return cs.atualizar(cm);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{id}")

    @Operation(summary = "Exclui um curso", description = "Remove um curso do sistema.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Curso excluído com sucesso"),
        @ApiResponse(responseCode = "404", description = "Curso não encontrado para o ID fornecido")
    })

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "ID do curso a ser excluído") @PathVariable Integer id) {
        return cs.excluir(id);
    }
}