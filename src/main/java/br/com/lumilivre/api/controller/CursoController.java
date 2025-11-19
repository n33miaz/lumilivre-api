package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.dto.requests.CursoRequestDTO;
import br.com.lumilivre.api.dto.responses.CursoResponseDTO;
import br.com.lumilivre.api.dto.ListaCursoDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.CursoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cursos")
@Tag(name = "11. Cursos")
@SecurityRequirement(name = "bearerAuth")
public class CursoController {

    @Autowired
    private CursoService cs;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    @Operation(summary = "Lista cursos para a tela principal do admin")
    public ResponseEntity<Page<ListaCursoDTO>> buscarCursosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaCursoDTO> cursos = cs.buscarCursoParaListaAdmin(texto, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca cursos com paginação e filtro de texto")
    public ResponseEntity<Page<ListaCursoDTO>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaCursoDTO> cursos = cs.buscarPorTexto(texto, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de cursos")
    public ResponseEntity<Page<ListaCursoDTO>> buscarAvancado(
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<ListaCursoDTO> cursos = cs.buscarAvancado(nome, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo curso")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Curso cadastrado", content = @Content(schema = @Schema(implementation = CursoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<CursoResponseDTO> cadastrar(@RequestBody @Valid CursoRequestDTO dto) {
        return cs.cadastrar(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("atualizar/{id}")
    @Operation(summary = "Atualiza um curso existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Curso atualizado", content = @Content(schema = @Schema(implementation = CursoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Curso não encontrado")
    })
    public ResponseEntity<CursoResponseDTO> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid CursoRequestDTO dto) {
        return cs.atualizar(id, dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um curso")
    public ResponseEntity<ResponseModel> excluir(@PathVariable Integer id) {
        return cs.excluir(id);
    }
}