package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.security.CanAccessLoan;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/emprestimos")
@Tag(name = "8. Empréstimos")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/home")
    @Operation(summary = "Lista empréstimos para a tela principal do admin")
    public ResponseEntity<Page<EmprestimoListagemResponse>> listarParaAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<EmprestimoListagemResponse> emprestimos = loanService.buscarEmprestimoParaListaAdmin(pageable);
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/buscar/ativos-e-atrasados")
    @Operation(summary = "Lista todos os empréstimos ativos e atrasados")
    public ResponseEntity<List<EmprestimoAtivoResponse>> buscarAtivosEAtrasados() {
        List<EmprestimoAtivoResponse> emprestimos = loanService.buscarAtivosEAtrasados();
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/buscar/apenas-atrasados")
    @Operation(summary = "Lista apenas os empréstimos com status ATRASADO")
    public ResponseEntity<List<EmprestimoAtivoResponse>> buscarApenasAtrasados() {
        List<EmprestimoAtivoResponse> emprestimos = loanService.buscarApenasAtrasados();
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca empréstimos com paginação e filtro de texto")
    public ResponseEntity<Page<EmprestimoListagemResponse>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<EmprestimoListagemResponse> emprestimos = loanService.buscarPorTexto(texto, pageable);
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de empréstimos")
    public ResponseEntity<Page<EmprestimoListagemResponse>> buscarAvancado(
            @RequestParam(required = false) LoanStatus statusEmprestimo,
            @RequestParam(required = false) String tombo,
            @RequestParam(required = false) String livroNome,
            @RequestParam(required = false) String alunoNome,
            @RequestParam(required = false) String dataEmprestimo,
            @RequestParam(required = false) String dataDevolucao,
            @RequestParam(required = false) String dataDevolucaoInicio,
            Pageable pageable) {

        OffsetDateTime dataDevInicio = null;
        if (dataDevolucaoInicio != null && !dataDevolucaoInicio.isBlank()) {
            dataDevInicio = LocalDate.parse(dataDevolucaoInicio)
                    .atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        }

        Page<EmprestimoListagemResponse> emprestimos = loanService.buscarAvancado(
                statusEmprestimo, tombo, livroNome, alunoNome,
                dataEmprestimo, dataDevolucao, dataDevInicio, pageable);

        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/contagem/ativos-e-atrasados")
    @Operation(summary = "Retorna a contagem de empréstimos ativos e atrasados")
    public ResponseEntity<Long> getContagemAtivosEAtrasados() {
        return ResponseEntity.ok(loanService.getContagemEmprestimosAtivosEAtrasados());
    }

    @GetMapping("/aluno/{matricula}")
    @CanAccessStudent
    @Operation(summary = "Lista os empréstimos ativos de um aluno")
    public ResponseEntity<List<EmprestimoResponse>> listarEmprestimos(@PathVariable String matricula) {
        return ResponseEntity.ok(loanService.listarEmprestimosAluno(matricula));
    }

    @GetMapping("/aluno/{matricula}/historico")
    @CanAccessStudent
    @Operation(summary = "Lista o histórico de empréstimos de um aluno")
    public ResponseEntity<List<EmprestimoResponse>> historicoEmprestimos(@PathVariable String matricula) {
        return ResponseEntity.ok(loanService.listarHistorico(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/dashboard")
    @Operation(summary = "Lista os empréstimos que estão vencendo para o dashboard")
    public List<EmprestimoDashboardResponse> listarVencendo() {
        return loanService.listarEmprestimosAtivosEAtrasados();
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    @Operation(summary = "Ranking de alunos por quantidade de empréstimos com filtros")
    public ResponseEntity<List<AlunoRankingResponse>> rankingAlunos(
            @RequestParam(defaultValue = "10") int top,
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) Integer moduloId,
            @RequestParam(required = false) Integer turnoId) {
        List<AlunoRankingResponse> ranking = loanService.gerarRankingAlunos(top, cursoId, moduloId, turnoId);
        return ranking.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(ranking);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Registra um novo empréstimo")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Empréstimo cadastrado com sucesso", content = @Content(schema = @Schema(implementation = EmprestimoResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Aluno ou Exemplar não encontrado")
    })
    public ResponseEntity<EmprestimoResponse> cadastrar(@RequestBody EmprestimoRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.cadastrar(dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um empréstimo existente")
    public ResponseEntity<EmprestimoResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody EmprestimoRequest dto) {
        dto.setId(id);
        return ResponseEntity.ok(loanService.atualizar(dto));
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PutMapping("/concluir/{id}")
    @Operation(summary = "Conclui (devolve) um empréstimo")
    public ResponseEntity<EmprestimoResponse> concluirEmprestimo(@PathVariable UUID id) {
        return ResponseEntity.ok(loanService.concluirEmprestimo(id));
    }

    @CanAccessLoan
    @PutMapping("/renovar/{id}")
    @Operation(summary = "Renova um empréstimo ativo por mais 14 dias")
    public ResponseEntity<EmprestimoResponse> renovar(@PathVariable UUID id) {
        return ResponseEntity.ok(loanService.renovar(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um registro de empréstimo")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable UUID id) {
        loanService.excluir(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Empréstimo excluído com sucesso.", null));
    }
}
