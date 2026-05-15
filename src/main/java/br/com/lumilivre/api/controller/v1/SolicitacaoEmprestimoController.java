package br.com.lumilivre.api.controller.v1;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.dto.v1.solicitacao.SolicitacaoCompletaResponse;
import br.com.lumilivre.api.dto.v1.solicitacao.SolicitacaoDashboardResponse;
import br.com.lumilivre.api.dto.v1.solicitacao.SolicitacaoResponse;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.LoanRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("solicitacoes")
@Tag(name = "7. Solicitações")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
@RequiredArgsConstructor
public class SolicitacaoEmprestimoController {

    private final LoanRequestService loanRequestService;

    @GetMapping("/dashboard")
    @Operation(summary = "Lista solicitações pendentes para o dashboard")
    public ResponseEntity<List<SolicitacaoDashboardResponse>> listarDashboard() {
        return ResponseEntity.ok(loanRequestService.listarSolicitacoesPendentes());
    }

    @GetMapping("/todas")
    @Operation(summary = "Lista o histórico completo de solicitações")
    public ResponseEntity<List<SolicitacaoCompletaResponse>> listarTodas() {
        return ResponseEntity.ok(loanRequestService.listarTodasSolicitacoes());
    }

    @CanAccessStudent
    @PostMapping("/solicitar")
    @Operation(summary = "Solicita empréstimo de um exemplar específico (Tombo)")
    public ResponseEntity<String> solicitar(
            @Parameter(description = "Matrícula do aluno solicitante") @RequestParam("matriculaAluno") String matricula,
            @Parameter(description = "Código do tombo do exemplar") @RequestParam String tomboExemplar) {
        return loanRequestService.solicitarEmprestimo(matricula, tomboExemplar);
    }

    @CanAccessStudent
    @PostMapping("/solicitar-mobile")
    @Operation(summary = "Solicita empréstimo via App Mobile (por ID do Livro)")
    public ResponseEntity<String> solicitarMobile(
            @Parameter(description = "Matrícula do aluno solicitante") @RequestParam("matriculaAluno") String matricula,
            @Parameter(description = "ID do livro desejado") @RequestParam UUID livroId) {
        return loanRequestService.solicitarEmprestimoPorLivro(matricula, livroId);
    }

    @PostMapping("/processar/{id}")
    @Operation(summary = "Processa uma solicitação (Aceitar ou Rejeitar)")
    public ResponseEntity<String> processar(
            @Parameter(description = "ID da solicitação") @PathVariable UUID id,
            @Parameter(description = "True para aceitar, False para rejeitar") @RequestParam boolean aceitar) {
        return loanRequestService.processarSolicitacao(id, aceitar);
    }

    @GetMapping("/pendentes")
    @Operation(summary = "Lista solicitações com status PENDENTE (DTO detalhado)")
    public List<SolicitacaoResponse> listarPendentes() {
        return loanRequestService.listarPendentesDTO();
    }

    @CanAccessStudent
    @GetMapping("/aluno/{matricula}")
    @Operation(summary = "Lista solicitações de um aluno específico")
    public List<SolicitacaoResponse> listarDoAluno(
            @Parameter(description = "Matrícula do aluno") @PathVariable String matricula) {
        return loanRequestService.listarSolicitacoesDoAlunoDTO(matricula);
    }
}
