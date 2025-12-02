package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.lumilivre.api.dto.aluno.AlunoRankingResponse;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoAtivoResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoDashboardResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoListagemResponse;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoResponse;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.service.EmprestimoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/emprestimos")
@Tag(name = "5. Empréstimos")
@SecurityRequirement(name = "bearerAuth")
public class EmprestimoController {

    @Autowired
    private EmprestimoService es;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    @Operation(summary = "Lista empréstimos para a tela principal do admin")
    public ResponseEntity<Page<EmprestimoListagemResponse>> listarParaAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<EmprestimoListagemResponse> emprestimos = es.buscarEmprestimoParaListaAdmin(pageable);
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/buscar/ativos-e-atrasados")
    @Operation(summary = "Lista todos os empréstimos ativos e atrasados")
    public ResponseEntity<List<EmprestimoAtivoResponse>> buscarAtivosEAtrasados() {
        List<EmprestimoAtivoResponse> emprestimos = es.buscarAtivosEAtrasados();
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/buscar/apenas-atrasados")
    @Operation(summary = "Lista apenas os empréstimos com status ATRASADO")
    public ResponseEntity<List<EmprestimoAtivoResponse>> buscarApenasAtrasados() {
        List<EmprestimoAtivoResponse> emprestimos = es.buscarApenasAtrasados();
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca empréstimos com paginação e filtro de texto")
    public ResponseEntity<Page<EmprestimoListagemResponse>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<EmprestimoListagemResponse> emprestimos = es.buscarPorTexto(texto, pageable);
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de empréstimos")
    public ResponseEntity<Page<EmprestimoListagemResponse>> buscarAvancado(
            @RequestParam(required = false) StatusEmprestimo statusEmprestimo,
            @RequestParam(required = false) String tombo,
            @RequestParam(required = false) String livroNome,
            @RequestParam(required = false) String alunoNome,
            @RequestParam(required = false) String dataEmprestimo,
            @RequestParam(required = false) String dataDevolucao,
            @RequestParam(required = false) String dataDevolucaoInicio,
            Pageable pageable) {

        LocalDateTime dataDevInicio = null;
        if (dataDevolucaoInicio != null && !dataDevolucaoInicio.isBlank()) {
            dataDevInicio = LocalDate.parse(dataDevolucaoInicio).atStartOfDay();
        }

        Page<EmprestimoListagemResponse> emprestimos = es.buscarAvancado(
                statusEmprestimo,
                tombo,
                livroNome,
                alunoNome,
                dataEmprestimo,
                dataDevolucao,
                dataDevInicio,
                pageable);

        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/contagem/ativos-e-atrasados")
    @Operation(summary = "Retorna a contagem de empréstimos ativos e atrasados")
    public ResponseEntity<Long> getContagemAtivosEAtrasados() {
        return ResponseEntity.ok(es.getContagemEmprestimosAtivosEAtrasados());
    }

    @GetMapping("/aluno/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @Operation(summary = "Lista os empréstimos ativos de um aluno")
    public ResponseEntity<List<EmprestimoResponse>> listarEmprestimos(
            @PathVariable String matricula) {
        return ResponseEntity.ok(es.listarEmprestimosAluno(matricula));
    }

    @GetMapping("/aluno/{matricula}/historico")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @Operation(summary = "Lista o histórico de empréstimos de um aluno")
    public ResponseEntity<List<EmprestimoResponse>> historicoEmprestimos(
            @PathVariable String matricula) {
        return ResponseEntity.ok(es.listarHistorico(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/dashboard")
    @Operation(summary = "Lista os empréstimos que estão vencendo para o dashboard")
    public List<EmprestimoDashboardResponse> listarVencendo() {
        return es.listarEmprestimosAtivosEAtrasados();
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @Operation(summary = "Ranking de alunos por quantidade de empréstimos com filtros")
    public ResponseEntity<List<AlunoRankingResponse>> rankingAlunos(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) Integer moduloId,
            @RequestParam(required = false) Integer turnoId) {
        List<AlunoRankingResponse> ranking = es.gerarRankingAlunos(top, cursoId, moduloId, turnoId);
        return ranking.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(ranking);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Registra um novo empréstimo")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Empréstimo cadastrado com sucesso", content = @Content(schema = @Schema(implementation = EmprestimoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno ou Exemplar não encontrado")
    })
    public ResponseEntity<EmprestimoResponse> cadastrar(@RequestBody EmprestimoRequest dto) {
        EmprestimoResponse novoEmprestimo = es.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoEmprestimo);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um empréstimo existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Empréstimo atualizado com sucesso", content = @Content(schema = @Schema(implementation = EmprestimoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Empréstimo já concluído ou inválido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    })
    public ResponseEntity<EmprestimoResponse> atualizar(
            @PathVariable Integer id,
            @RequestBody EmprestimoRequest dto) {
        dto.setId(id);
        EmprestimoResponse atualizado = es.atualizar(dto);
        return ResponseEntity.ok(atualizado);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/concluir/{id}")
    @Operation(summary = "Conclui (devolve) um empréstimo")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Empréstimo concluído com sucesso", content = @Content(schema = @Schema(implementation = EmprestimoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Empréstimo não encontrado ou já concluído")
    })
    public ResponseEntity<EmprestimoResponse> concluirEmprestimo(@PathVariable Integer id) {
        EmprestimoResponse concluido = es.concluirEmprestimo(id);
        return ResponseEntity.ok(concluido);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um registro de empréstimo")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Empréstimo excluído com sucesso")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id) {
        es.excluir(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Empréstimo excluído com sucesso.", null));
    }
}