package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.EmprestimoDTO;
import br.com.lumilivre.api.data.EmprestimoResponseDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.EmprestimoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/emprestimos")

@Tag(name = "5. Empréstimos")
@SecurityRequirement(name = "bearerAuth")

public class EmprestimoController {

    @Autowired
    private EmprestimoService es;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")

    @Operation(summary = "Busca empréstimos com paginação e filtro de texto", description = "Retorna uma página de empréstimos. Pode filtrar por um texto genérico.")
    @ApiResponse(responseCode = "200", description = "Página de empréstimos retornada")

    public ResponseEntity<Page<EmprestimoModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<EmprestimoModel> emprestimos = es.buscarPorTexto(texto, pageable);

        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")

    @Operation(summary = "Busca avançada e paginada de empréstimos", description = "Filtra empréstimos por campos específicos.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Página de empréstimos encontrada"),
        @ApiResponse(responseCode = "204", description = "Nenhum empréstimo encontrado para os filtros")
    })

    public ResponseEntity<Page<EmprestimoModel>> buscarAvancado(
                @Parameter(description = "Status do empréstimo (ATIVO, CONCLUIDO, ATRASADO)") @RequestParam(required = false) StatusEmprestimo statusEmprestimo,
                @Parameter(description = "Código de tombo do exemplar") @RequestParam(required = false) String tombo,
                @Parameter(description = "Nome parcial do livro") @RequestParam(required = false) String livroNome,
                @Parameter(description = "Nome parcial do aluno") @RequestParam(required = false) String alunoNome,
                @Parameter(description = "Data do empréstimo (formato YYYY-MM-DD)") @RequestParam(required = false) String dataEmprestimo,
                @Parameter(description = "Data da devolução (formato YYYY-MM-DD)") @RequestParam(required = false) String dataDevolucao,
            Pageable pageable) {

        Page<EmprestimoModel> emprestimos = es.buscarAvancado(
                statusEmprestimo,
                tombo,
                livroNome,
                alunoNome,
                dataEmprestimo,
                dataDevolucao,
                pageable);

        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("buscar/ativos")

    @Operation(summary = "Lista todos os empréstimos ativos", description = "Retorna uma lista de todos os empréstimos com status ATIVO.")
    @ApiResponse(responseCode = "200", description = "Lista de empréstimos ativos")

    public ResponseEntity<List<EmprestimoModel>> buscarAtivos() {
        return ResponseEntity.ok(es.buscarAtivos());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @GetMapping("/buscar/ativos-e-atrasados") 
    @Operation(summary = "Lista todos os empréstimos ativos e atrasados", description = "Retorna uma lista de todos os empréstimos que não foram concluídos.")
    public ResponseEntity<List<EmprestimoModel>> buscarAtivosEAtrasados() {
        return ResponseEntity.ok(es.buscarAtivosEAtrasados());
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @GetMapping("/contagem/ativos-e-atrasados") 
    @Operation(summary = "Retorna a contagem de empréstimos ativos e atrasados")
    public ResponseEntity<Long> getContagemAtivosEAtrasados() {
        return ResponseEntity.ok(es.getContagemEmprestimosAtivosEAtrasados());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @GetMapping("/buscar/apenas-atrasados") 
    @Operation(summary = "Lista apenas os empréstimos com status ATRASADO")
    public ResponseEntity<List<EmprestimoModel>> buscarApenasAtrasados() {
        return ResponseEntity.ok(es.buscarApenasAtrasados());
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("buscar/concluidos")

    @Operation(summary = "Lista todos os empréstimos concluídos", description = "Retorna uma lista de todos os empréstimos com status CONCLUIDO.")
    @ApiResponse(responseCode = "200", description = "Lista de empréstimos concluídos")

    public ResponseEntity<List<EmprestimoModel>> buscarConcluidos() {
        return ResponseEntity.ok(es.buscarConcluidos());
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/aluno/{matricula}")

    @Operation(summary = "Lista os empréstimos ativos de um aluno", description = "Retorna uma lista de empréstimos com status ATIVO para uma matrícula de aluno específica.")
    @ApiResponse(responseCode = "200", description = "Empréstimos encontrados", content = @Content(schema = @Schema(implementation = EmprestimoResponseDTO.class)))

    public ResponseEntity<?> listarEmprestimos(
            @Parameter(description = "Matrícula do aluno") @PathVariable String matricula) {
        return ResponseEntity.ok(es.listarEmprestimosAluno(matricula));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/aluno/{matricula}/historico")

    @Operation(summary = "Lista o histórico de empréstimos de um aluno", description = "Retorna uma lista de todos os empréstimos com status CONCLUIDO para uma matrícula de aluno específica.")
    @ApiResponse(responseCode = "200", description = "Histórico encontrado", content = @Content(schema = @Schema(implementation = EmprestimoResponseDTO.class)))

    public ResponseEntity<?> historicoEmprestimos(
            @Parameter(description = "Matrícula do aluno") @PathVariable String matricula) {
        return ResponseEntity.ok(es.listarHistorico(matricula));
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Registra um novo empréstimo", description = "Cria um novo registro de empréstimo, associando um exemplar a um aluno.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Empréstimo cadastrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: aluno ou exemplar não encontrado, exemplar indisponível, aluno com limite de empréstimos)")
    })

    public ResponseEntity<?> cadastrar(@RequestBody EmprestimoDTO dto) {
        return es.cadastrar(dto);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{id}")

    @Operation(summary = "Atualiza um empréstimo existente", description = "Altera os dados de um empréstimo, como as datas. Não altera o status.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Empréstimo atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Empréstimo já concluído ou não encontrado")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "ID do empréstimo a ser atualizado") @PathVariable Integer id, 
            @RequestBody EmprestimoDTO dto) {
        dto.setId(id);
        return es.atualizar(dto);
    }
    
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/concluir/{id}")

    @Operation(summary = "Conclui (devolve) um empréstimo", description = "Muda o status de um empréstimo para CONCLUIDO, calcula possíveis penalidades e libera o exemplar.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Empréstimo concluído com sucesso"),
        @ApiResponse(responseCode = "400", description = "Empréstimo não encontrado ou já concluído")
    })

    public ResponseEntity<?> concluirEmprestimo(
            @Parameter(description = "ID do empréstimo a ser concluído") @PathVariable Integer id) {
        return es.concluirEmprestimo(id);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{id}")

    @Operation(summary = "Exclui um registro de empréstimo", description = "Remove um registro de empréstimo do sistema. Use com cautela.")
    @ApiResponse(responseCode = "200", description = "Empréstimo excluído com sucesso")

    public ResponseEntity<ResponseModel> excluir(@PathVariable Integer id) {
        return es.excluir(id);
    }
}