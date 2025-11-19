package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.dto.EmprestimoDTO;
import br.com.lumilivre.api.dto.ListaEmprestimoAtivoDTO;
import br.com.lumilivre.api.dto.ListaEmprestimoDTO;
import br.com.lumilivre.api.dto.ListaEmprestimoDashboardDTO;
import br.com.lumilivre.api.dto.aluno.AlunoRankingResponse;
import br.com.lumilivre.api.dto.responses.EmprestimoResponseDTO;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.EmprestimoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    public ResponseEntity<Page<ListaEmprestimoDTO>> listarParaAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaEmprestimoDTO> emprestimos = es.buscarEmprestimoParaListaAdmin(pageable);
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/buscar/ativos-e-atrasados")
    @Operation(summary = "Lista todos os empréstimos ativos e atrasados")
    public ResponseEntity<List<ListaEmprestimoAtivoDTO>> buscarAtivosEAtrasados() {
        List<ListaEmprestimoAtivoDTO> emprestimos = es.buscarAtivosEAtrasados();
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/buscar/apenas-atrasados")
    @Operation(summary = "Lista apenas os empréstimos com status ATRASADO")
    public ResponseEntity<List<ListaEmprestimoAtivoDTO>> buscarApenasAtrasados() {
        List<ListaEmprestimoAtivoDTO> emprestimos = es.buscarApenasAtrasados();
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca empréstimos com paginação e filtro de texto")
    public ResponseEntity<Page<ListaEmprestimoDTO>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaEmprestimoDTO> emprestimos = es.buscarPorTexto(texto, pageable);
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de empréstimos")
    public ResponseEntity<Page<ListaEmprestimoDTO>> buscarAvancado(
            @RequestParam(required = false) StatusEmprestimo statusEmprestimo,
            @RequestParam(required = false) String tombo,
            @RequestParam(required = false) String livroNome,
            @RequestParam(required = false) String alunoNome,
            @RequestParam(required = false) String dataEmprestimo,
            @RequestParam(required = false) String dataDevolucao,
            Pageable pageable) {

        Page<ListaEmprestimoDTO> emprestimos = es.buscarAvancado(
                statusEmprestimo, tombo, livroNome, alunoNome, dataEmprestimo, dataDevolucao, pageable);
        return emprestimos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(emprestimos);
    }

    @GetMapping("/contagem/ativos-e-atrasados")
    @Operation(summary = "Retorna a contagem de empréstimos ativos e atrasados")
    public ResponseEntity<Long> getContagemAtivosEAtrasados() {
        return ResponseEntity.ok(es.getContagemEmprestimosAtivosEAtrasados());
    }

    @GetMapping("/aluno/{matricula}")
    @Operation(summary = "Lista os empréstimos ativos de um aluno")
    public ResponseEntity<List<br.com.lumilivre.api.dto.EmprestimoResponseDTO>> listarEmprestimos(
            @PathVariable String matricula) {
        return ResponseEntity.ok(es.listarEmprestimosAluno(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/aluno/{matricula}/historico")
    @Operation(summary = "Lista o histórico de empréstimos de um aluno")
    public ResponseEntity<List<br.com.lumilivre.api.dto.EmprestimoResponseDTO>> historicoEmprestimos(
            @PathVariable String matricula) {
        return ResponseEntity.ok(es.listarHistorico(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/dashboard")
    @Operation(summary = "Lista os empréstimos que estão vencendo para o dashboard")
    public List<ListaEmprestimoDashboardDTO> listarVencendo() {
        return es.listarEmprestimosAtivosEAtrasados();
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/ranking")
    @Operation(summary = "Ranking de alunos por quantidade de empréstimos")
    public ResponseEntity<List<AlunoRankingResponse>> rankingAlunos(@RequestParam(defaultValue = "10") int top) {
        List<AlunoRankingResponse> ranking = es.gerarRankingAlunos(top);
        return ranking.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(ranking);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Registra um novo empréstimo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empréstimo cadastrado com sucesso", content = @Content(schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
            @ApiResponse(responseCode = "404", description = "Aluno ou Exemplar não encontrado")
    })
    public ResponseEntity<EmprestimoResponseDTO> cadastrar(@RequestBody EmprestimoDTO dto) {
        EmprestimoResponseDTO novoEmprestimo = es.cadastrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoEmprestimo);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um empréstimo existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empréstimo atualizado com sucesso", content = @Content(schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Empréstimo já concluído ou inválido"),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    })
    public ResponseEntity<EmprestimoResponseDTO> atualizar(
            @PathVariable Integer id,
            @RequestBody EmprestimoDTO dto) {
        dto.setId(id);
        EmprestimoResponseDTO atualizado = es.atualizar(dto);
        return ResponseEntity.ok(atualizado);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/concluir/{id}")
    @Operation(summary = "Conclui (devolve) um empréstimo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empréstimo concluído com sucesso", content = @Content(schema = @Schema(implementation = EmprestimoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Empréstimo não encontrado ou já concluído")
    })
    public ResponseEntity<EmprestimoResponseDTO> concluirEmprestimo(@PathVariable Integer id) {
        EmprestimoResponseDTO concluido = es.concluirEmprestimo(id);
        return ResponseEntity.ok(concluido);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um registro de empréstimo")
    @ApiResponse(responseCode = "200", description = "Empréstimo excluído com sucesso")
    public ResponseEntity<ResponseModel> excluir(@PathVariable Integer id) {
        es.excluir(id);
        return ResponseEntity.ok(new ResponseModel("Empréstimo excluído com sucesso."));
    }
}