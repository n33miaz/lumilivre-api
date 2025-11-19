package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.AlunoDTO;
import br.com.lumilivre.api.dto.responses.AlunoResponseDTO; // Importe o novo DTO
import br.com.lumilivre.api.dto.ListaAlunoDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/alunos")
@Tag(name = "6. Alunos")
@SecurityRequirement(name = "bearerAuth")
public class AlunoController {

    private final AlunoService alunoService;

    public AlunoController(AlunoService alunoService) {
        this.alunoService = alunoService;
    }

    // --- (GET) ---

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Lista alunos para a tela principal do admin")
    public ResponseEntity<Page<ListaAlunoDTO>> listarParaAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaAlunoDTO> alunos = alunoService.buscarAlunosParaListaAdmin(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca alunos com paginação e filtro de texto")
    public ResponseEntity<Page<ListaAlunoDTO>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaAlunoDTO> alunos = alunoService.buscarPorTexto(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar/avancado")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca avançada e paginada de alunos")
    public ResponseEntity<Page<ListaAlunoDTO>> buscarAvancado(
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

        Page<ListaAlunoDTO> alunos = alunoService.buscarAvancado(penalidade, matricula, nome, cursoNome, turnoId,
                moduloId, dataNascimento, email, celular, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca detalhes de um aluno específico")
    public ResponseEntity<AlunoResponseDTO> buscarPorMatricula(@PathVariable String matricula) {
        AlunoModel aluno = alunoService.buscarPorMatricula(matricula);
        return ResponseEntity.ok(new AlunoResponseDTO(aluno));
    }

    // --- (POST, PUT, DELETE) ---

    @PostMapping("/cadastrar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Cadastra um novo aluno")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aluno cadastrado com sucesso", content = @Content(schema = @Schema(implementation = AlunoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<AlunoResponseDTO> cadastrar(@RequestBody @Valid AlunoDTO alunoDTO) {
        AlunoModel alunoSalvo = alunoService.cadastrar(alunoDTO);

        AlunoResponseDTO response = new AlunoResponseDTO(alunoSalvo);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/atualizar/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Atualiza um aluno existente")
    public ResponseEntity<AlunoResponseDTO> atualizar(
            @PathVariable String matricula,
            @RequestBody @Valid AlunoDTO alunoDTO) {

        AlunoModel alunoAtualizado = alunoService.atualizar(matricula, alunoDTO);
        return ResponseEntity.ok(new AlunoResponseDTO(alunoAtualizado));
    }

    @DeleteMapping("/excluir/{matricula}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclui um aluno (Acesso: ADMIN)")
    public ResponseEntity<ResponseModel> excluir(@PathVariable String matricula) {
        alunoService.excluir(matricula);
        return ResponseEntity.ok(new ResponseModel("Aluno excluído com sucesso."));
    }
}