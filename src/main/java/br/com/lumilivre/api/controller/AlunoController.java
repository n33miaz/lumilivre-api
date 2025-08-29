package br.com.lumilivre.api.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.data.AlunoDTO;
import br.com.lumilivre.api.data.ListaAlunoDTO;
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
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/alunos")
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")

@Tag(name = "2. Alunos")
@SecurityRequirement(name = "bearerAuth") // endpoint exige autenticação JWT (anotação adiciona um icone de cadeado)

public class AlunoController {

    @Autowired
    private AlunoService as;

    public AlunoController(AlunoService AlunoService) {
        this.as = AlunoService;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/todos")
    public Iterable<AlunoModel> buscar() {
        return as.buscar();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")

    @Operation(summary = "Busca alunos com paginação e filtro de texto", description = "Retorna uma página de alunos. Pode filtrar por um texto genérico que busca em múltiplos campos.")
    @ApiResponse(responseCode = "200", description = "Página de alunos retornada com sucesso")

    public ResponseEntity<Page<AlunoModel>> buscarPorTexto(

            @Parameter(description = "Texto para busca genérica (em nome, matrícula, etc.)") @RequestParam(required = false) String texto,

            Pageable pageable) {
        Page<AlunoModel> alunos = as.buscarPorTexto(texto, pageable);
        if (alunos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alunos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")

    @Operation(summary = "Busca avançada e paginada de alunos", description = "Filtra alunos por campos específicos como nome, matrícula, data de nascimento e nome do curso.")
    @ApiResponses
    ({
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
        if (alunos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(alunos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo aluno", description = "Cria um novo aluno e seu usuário correspondente no sistema.")
    @ApiResponse(responseCode = "201", description = "Aluno cadastrado com sucesso", content = @Content(schema = @Schema(implementation = AlunoModel.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: CPF inválido, matrícula duplicada)")

    public ResponseEntity<?> cadastrar(@RequestBody AlunoDTO alunoDTO) {
        return as.cadastrar(alunoDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable String id, @RequestBody AlunoDTO alunoDTO) {
        // depois, quando implementar JWT, validar se ALUNO só altera seu próprio RM
        return as.atualizar(id, alunoDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/excluir/{id}")

    @Operation(summary = "Exclui um aluno (Acesso: ADMIN)", description = "Remove um aluno do sistema. Esta é uma operação destrutiva e requer permissão de Administrador.")
    @ApiResponse(responseCode = "200", description = "Aluno excluído com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não é ADMIN)")

    public ResponseEntity<ResponseModel> excluir(
        
        @Parameter(description = "Matrícula do aluno a ser excluído") @PathVariable String id) {

        return as.excluir(id);
    }
}
