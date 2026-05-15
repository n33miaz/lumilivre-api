package br.com.lumilivre.api.controller.v1;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.lumilivre.api.dto.v1.comum.ApiResponse;
import br.com.lumilivre.api.dto.v1.curso.CursoRequest;
import br.com.lumilivre.api.dto.v1.curso.CursoResponse;
import br.com.lumilivre.api.dto.v1.curso.CursoResumoResponse;
import br.com.lumilivre.api.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cursos")
@Tag(name = "9. Cursos")
@SecurityRequirement(name = "bearerAuth")
public class CursoController {

    private final CourseService courseService;

    public CursoController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    @Operation(summary = "Lista cursos para a tela principal do admin")
    public ResponseEntity<Page<CursoResumoResponse>> buscarCursosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<CursoResumoResponse> cursos = courseService.buscarCursoParaListaAdmin(texto, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca cursos com paginação e filtro de texto")
    public ResponseEntity<Page<CursoResumoResponse>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<CursoResumoResponse> cursos = courseService.buscarPorTexto(texto, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de cursos")
    public ResponseEntity<Page<CursoResumoResponse>> buscarAvancado(
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        Page<CursoResumoResponse> cursos = courseService.buscarAvancado(nome, pageable);
        return cursos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(cursos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo curso")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Curso cadastrado", content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<CursoResponse> cadastrar(@RequestBody @Valid CursoRequest dto) {
        return courseService.cadastrar(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PutMapping("atualizar/{id}")
    @Operation(summary = "Atualiza um curso existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Curso atualizado", content = @Content(schema = @Schema(implementation = CursoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Curso não encontrado")
    })
    public ResponseEntity<CursoResponse> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid CursoRequest dto) {
        return courseService.atualizar(id, dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um curso")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id) {
        return courseService.excluir(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/estatisticas")
    @Operation(summary = "Retorna estatísticas gerais dos cursos (qtd alunos e total empréstimos)")
    public ResponseEntity<List<br.com.lumilivre.api.dto.v1.curso.CursoEstatisticaResponse>> getEstatisticas() {
        return ResponseEntity.ok(courseService.buscarEstatisticas());
    }

    @GetMapping("/estatisticas-grafico")
    public ResponseEntity<List<br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse>> getEstatisticasGrafico() {
        return ResponseEntity.ok(courseService.buscarTotalEmprestimosPorCurso());
    }
}
