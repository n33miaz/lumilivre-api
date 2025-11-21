package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.lumilivre.api.dto.aluno.AlunoRequest;
import br.com.lumilivre.api.dto.aluno.AlunoResponse;
import br.com.lumilivre.api.dto.aluno.AlunoResumoResponse;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/alunos")
@Tag(name = "6. Alunos")
@SecurityRequirement(name = "bearerAuth")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Lista alunos para a tela principal do admin")
    public ResponseEntity<Page<AlunoResumoResponse>> listarParaAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AlunoResumoResponse> alunos = alunoService.buscarAlunosParaListaAdmin(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca alunos com paginação e filtro de texto")
    public ResponseEntity<Page<AlunoResumoResponse>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AlunoResumoResponse> alunos = alunoService.buscarPorTexto(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar/avancado")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca avançada e paginada de alunos")
    public ResponseEntity<Page<AlunoResumoResponse>> buscarAvancado(
            @RequestParam(required = false) String penalidade,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String cursoNome,
            @RequestParam(required = false) Integer turnoId,
            @RequestParam(required = false) Integer moduloId,
            @RequestParam(required = false) LocalDate dataNascimento,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String celular,
            Pageable pageable) {

        Page<AlunoResumoResponse> alunos = alunoService.buscarAvancado(penalidade, matricula, nome, cursoNome, turnoId,
                moduloId, dataNascimento, email, celular, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca detalhes de um aluno específico")
    public ResponseEntity<ApiResponse<AlunoResponse>> buscarPorMatricula(@PathVariable String matricula) {
        AlunoModel aluno = alunoService.buscarPorMatricula(matricula);
        return ResponseEntity.ok(new ApiResponse<>(true, "Aluno encontrado", new AlunoResponse(aluno)));
    }

    @PostMapping("/cadastrar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Cadastra um novo aluno")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Aluno cadastrado com sucesso", content = @Content(schema = @Schema(implementation = AlunoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<ApiResponse<AlunoResponse>> cadastrar(@RequestBody @Valid AlunoRequest alunoDTO) {
        AlunoModel alunoSalvo = alunoService.cadastrar(alunoDTO);
        AlunoResponse response = new AlunoResponse(alunoSalvo);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Aluno cadastrado com sucesso", response));
    }

    @PutMapping("/atualizar/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Atualiza um aluno existente")
    public ResponseEntity<ApiResponse<AlunoResponse>> atualizar(
            @PathVariable String matricula,
            @RequestBody @Valid AlunoRequest alunoDTO) {

        AlunoModel alunoAtualizado = alunoService.atualizar(matricula, alunoDTO);

        return ResponseEntity
                .ok(new ApiResponse<>(true, "Aluno atualizado com sucesso", new AlunoResponse(alunoAtualizado)));
    }

    @DeleteMapping("/excluir/{matricula}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclui um aluno (Acesso: ADMIN)")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable String matricula) {
        alunoService.excluir(matricula);
        return ResponseEntity.ok(new ApiResponse<>(true, "Aluno excluído com sucesso.", null));
    }
}