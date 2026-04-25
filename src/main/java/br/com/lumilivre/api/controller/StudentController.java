package br.com.lumilivre.api.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.dto.aluno.AlunoRequest;
import br.com.lumilivre.api.dto.aluno.AlunoResponse;
import br.com.lumilivre.api.dto.aluno.AlunoResumoResponse;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/alunos")
@Tag(name = "3. Alunos")
@SecurityRequirement(name = "bearerAuth")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Lista alunos para a tela principal do admin")
    public ResponseEntity<Page<AlunoResumoResponse>> listarParaAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AlunoResumoResponse> alunos = studentService.buscarAlunosParaListaAdmin(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Busca alunos com paginação e filtro de texto")
    public ResponseEntity<Page<AlunoResumoResponse>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AlunoResumoResponse> alunos = studentService.buscarPorTexto(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar/avancado")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
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

        Page<AlunoResumoResponse> alunos = studentService.buscarAvancado(
                penalidade, matricula, nome, cursoNome, turnoId, moduloId, dataNascimento, email, celular, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/{matricula}")
    @CanAccessStudent
    @Operation(summary = "Busca detalhes de um aluno específico")
    public ResponseEntity<ApiResponse<AlunoResponse>> buscarPorMatricula(@PathVariable String matricula) {
        Student student = studentService.buscarPorMatricula(matricula);
        return ResponseEntity.ok(new ApiResponse<>(true, "Aluno encontrado", new AlunoResponse(student)));
    }

    @PostMapping(value = "/{matricula}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CanAccessStudent
    @Operation(summary = "Atualiza a foto de perfil do aluno")
    public ResponseEntity<ApiResponse<Void>> uploadFoto(
            @PathVariable String matricula,
            @RequestParam("file") MultipartFile file) {

        studentService.uploadFoto(matricula, file);
        return ResponseEntity.ok(new ApiResponse<>(true, "Foto atualizada com sucesso.", null));
    }

    @PostMapping("/cadastrar")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Cadastra um novo aluno")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Aluno cadastrado com sucesso", content = @Content(schema = @Schema(implementation = AlunoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    public ResponseEntity<ApiResponse<AlunoResponse>> cadastrar(@RequestBody @Valid AlunoRequest alunoDTO) {
        Student savedStudent = studentService.cadastrar(alunoDTO);
        AlunoResponse response = new AlunoResponse(savedStudent);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Aluno cadastrado com sucesso", response));
    }

    @PutMapping("/atualizar/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Atualiza um aluno existente")
    public ResponseEntity<ApiResponse<AlunoResponse>> atualizar(
            @PathVariable String matricula,
            @RequestBody @Valid AlunoRequest alunoDTO) {

        Student updatedStudent = studentService.atualizar(matricula, alunoDTO);

        return ResponseEntity.ok(new ApiResponse<>(true, "Aluno atualizado com sucesso", new AlunoResponse(updatedStudent)));
    }

    @PatchMapping("/{matricula}/reset-senha")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Reseta a senha do aluno para a matrícula")
    public ResponseEntity<ApiResponse<Void>> resetarSenha(@PathVariable String matricula) {
        studentService.resetarSenha(matricula);
        return ResponseEntity.ok(new ApiResponse<>(true, "Senha resetada para a matrícula com sucesso.", null));
    }

    @DeleteMapping("/excluir/{matricula}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclui um aluno (Acesso: ADMIN)")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable String matricula) {
        studentService.excluir(matricula);
        return ResponseEntity.ok(new ApiResponse<>(true, "Aluno excluído com sucesso.", null));
    }
}
