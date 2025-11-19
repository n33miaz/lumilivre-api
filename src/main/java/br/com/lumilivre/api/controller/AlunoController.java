package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.AlunoDTO;
import br.com.lumilivre.api.dto.ListaAlunoDTO;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.AlunoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Lista alunos para a tela principal do admin", description = "Retorna uma lista paginada de alunos com dados resumidos para a exibição no dashboard. Suporta filtro de texto.")
    @ApiResponse(responseCode = "200", description = "Página de alunos retornada com sucesso")
    public ResponseEntity<Page<ListaAlunoDTO>> listarParaAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaAlunoDTO> alunos = alunoService.buscarAlunosParaListaAdmin(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca alunos com paginação e filtro de texto", description = "Retorna uma página de alunos com detalhes completos.")
    @ApiResponse(responseCode = "200", description = "Página de alunos retornada com sucesso")
    public ResponseEntity<Page<AlunoModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica (em nome, matrícula, etc.)") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AlunoModel> alunos = alunoService.buscarPorTexto(texto, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @GetMapping("/buscar/avancado")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Busca avançada e paginada de alunos", description = "Filtra alunos por campos específicos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de alunos encontrada"),
            @ApiResponse(responseCode = "204", description = "Nenhum aluno encontrado para os filtros fornecidos")
    })
    public ResponseEntity<Page<AlunoModel>> buscarAvancado(
            @Parameter(description = "Status da Penalidade (ADVERTENCIA, SUSPENSAO, etc)") @RequestParam(required = false) String penalidade,
            @Parameter(description = "Matrícula exata do aluno") @RequestParam(required = false) String matricula,
            @Parameter(description = "Nome parcial do aluno") @RequestParam(required = false) String nome,
            @Parameter(description = "Nome parcial do curso") @RequestParam(required = false) String cursoNome,
            @Parameter(description = "ID do turno do aluno") @RequestParam(required = false) Integer turnoId,
            @Parameter(description = "ID do módulo do aluno") @RequestParam(required = false) Integer moduloId,
            @Parameter(description = "Data de nascimento no formato YYYY-MM-DD") @RequestParam(required = false) LocalDate dataNascimento,
            @Parameter(description = "Texto parcial ou completo do email") @RequestParam(required = false) String email,
            @Parameter(description = "Número parcial ou completo do celular") @RequestParam(required = false) String celular,
            Pageable pageable) {

        Page<AlunoModel> alunos = alunoService.buscarAvancado(penalidade, matricula, nome, cursoNome, turnoId, moduloId,
                dataNascimento, email, celular, pageable);
        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    }

    @PostMapping("/cadastrar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Cadastra um novo aluno", description = "Cria um novo aluno e seu usuário correspondente no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aluno cadastrado com sucesso", content = @Content(schema = @Schema(implementation = AlunoModel.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Conflito de dados (matrícula ou CPF já existe)")
    })
    public ResponseEntity<?> cadastrar(@RequestBody AlunoDTO alunoDTO) {
        return alunoService.cadastrar(alunoDTO);
    }

    @PutMapping("/atualizar/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @Operation(summary = "Atualiza um aluno existente", description = "Altera os dados de um aluno com base na sua matrícula.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aluno atualizado com sucesso", content = @Content(schema = @Schema(implementation = AlunoModel.class))),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado para a matrícula fornecida")
    })
    public ResponseEntity<?> atualizar(
            @Parameter(description = "Matrícula do aluno a ser atualizado") @PathVariable String matricula,
            @RequestBody AlunoDTO alunoDTO) {
        return alunoService.atualizar(matricula, alunoDTO);
    }

    @DeleteMapping("/excluir/{matricula}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exclui um aluno (Acesso: ADMIN)", description = "Remove um aluno e seu usuário associado do sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aluno excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    })
    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "Matrícula do aluno a ser excluído") @PathVariable String matricula) {
        return alunoService.excluir(matricula);
    }
}