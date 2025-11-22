package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.curso.CursoRequest;
import br.com.lumilivre.api.dto.curso.CursoResponse;
import br.com.lumilivre.api.dto.curso.CursoResumoResponse;
import br.com.lumilivre.api.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cursos")
@Tag(name = "11. Cursos")
@SecurityRequirement(name = "bearerAuth")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    @Operation(summary = "Lista cursos para a tela principal do admin")
    public ResponseEntity<Page<CursoResumoResponse>> buscarCursosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<CursoResumoResponse> cursos = cursoService.buscarCursoParaListaAdmin(texto, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca cursos com paginação e filtro de texto")
    public ResponseEntity<Page<CursoResumoResponse>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<CursoResumoResponse> cursos = cursoService.buscarPorTexto(texto, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de cursos")
    public ResponseEntity<Page<CursoResumoResponse>> buscarAvancado(
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<CursoResumoResponse> cursos = cursoService.buscarAvancado(nome, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo curso")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Curso cadastrado", content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<CursoResponse> cadastrar(@RequestBody @Valid CursoRequest dto) {
        return cursoService.cadastrar(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("atualizar/{id}")
    @Operation(summary = "Atualiza um curso existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Curso atualizado", content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Curso não encontrado")
    })
    public ResponseEntity<CursoResponse> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid CursoRequest dto) {
        return cursoService.atualizar(id, dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um curso")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id) {
        return cursoService.excluir(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/estatisticas")
    @Operation(summary = "Retorna estatísticas gerais dos cursos (qtd alunos e total empréstimos)")
    public ResponseEntity<List<br.com.lumilivre.api.dto.curso.CursoEstatisticaResponse>> getEstatisticas() {
        return ResponseEntity.ok(cursoService.buscarEstatisticas());
    }
}