package br.com.lumilivre.api.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.AlunoDTO;
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

@RestController
@RequestMapping("/alunos")

@Tag(name = "6. Alunos")
@SecurityRequirement(name = "bearerAuth")

public class AlunoController {

    @Autowired
    private AlunoService as;

    public AlunoController(AlunoService AlunoService) {
        this.as = AlunoService;
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")

    @Operation(summary = "Busca alunos com paginação e filtro de texto", description = "Retorna uma página de alunos. Pode filtrar por um texto genérico que busca em múltiplos campos. Acesso: ADMIN, BIBLIOTECARIO.")
    @ApiResponse(responseCode = "200", description = "Página de alunos retornada com sucesso")

    public ResponseEntity<Page<AlunoModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica (em nome, matrícula, etc.)") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<AlunoModel> alunos = as.buscarPorTexto(texto, pageable);

        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    } 

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @GetMapping("/buscar/avancado")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")

    @Operation(summary = "Busca avançada e paginada de alunos", description = "Filtra alunos por campos específicos como nome, matrícula, data de nascimento e nome do curso. Acesso: ADMIN, BIBLIOTECARIO.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de alunos encontrada"),
        @ApiResponse(responseCode = "204", description = "Nenhum aluno encontrado para os filtros fornecidos")
    })

    public ResponseEntity<Page<AlunoModel>> buscarAvancado(
            @Parameter(description = "Nome parcial do aluno") @RequestParam(required = false) String nome,
            @Parameter(description = "Matrícula exata do aluno") @RequestParam(required = false) String matricula,
            @Parameter(description = "Data de nascimento no formato YYYY-MM-DD") @RequestParam(required = false) LocalDate dataNascimento,
            @Parameter(description = "Nome parcial do curso") @RequestParam(required = false) String cursoNome,
            Pageable pageable) {
        Page<AlunoModel> alunos = as.buscarAvancado(nome, matricula, dataNascimento, cursoNome, pageable);

        return alunos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(alunos);
    } 

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PostMapping("/cadastrar")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")

    @Operation(summary = "Cadastra um novo aluno", description = "Cria um novo aluno e seu usuário correspondente no sistema. Acesso: ADMIN, BIBLIOTECARIO.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Aluno cadastrado com sucesso", content = @Content(schema = @Schema(implementation = AlunoModel.class))),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (ex: CPF inválido, matrícula duplicada)")
    })

    public ResponseEntity<?> cadastrar(@RequestBody AlunoDTO alunoDTO) {
        return as.cadastrar(alunoDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PutMapping("/atualizar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")

    @Operation(summary = "Atualiza um aluno existente", description = "Altera os dados de um aluno com base na sua matrícula. Um ALUNO só pode alterar seus próprios dados (lógica a ser validada no serviço), enquanto ADMIN/BIBLIOTECARIO podem alterar qualquer um.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Aluno atualizado com sucesso", content = @Content(schema = @Schema(implementation = AlunoModel.class))),
        @ApiResponse(responseCode = "404", description = "Aluno não encontrado para a matrícula fornecida")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "Matrícula do aluno a ser atualizado") @PathVariable String id, 
            @RequestBody AlunoDTO alunoDTO) {
        return as.atualizar(id, alunoDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @DeleteMapping("/excluir/{id}")
    @PreAuthorize("hasRole('ADMIN')")

    @Operation(summary = "Exclui um aluno (Acesso: ADMIN)", description = "Remove um aluno do sistema. Esta é uma operação destrutiva e requer permissão de Administrador.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Aluno excluído com sucesso"),
        @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é ADMIN)")
    })

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "Matrícula do aluno a ser excluído") @PathVariable String id) {
        return as.excluir(id);
    }
}