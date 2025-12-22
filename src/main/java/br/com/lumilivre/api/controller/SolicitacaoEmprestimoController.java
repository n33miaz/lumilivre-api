package br.com.lumilivre.api.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoCompletaResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoResponse;
import br.com.lumilivre.api.service.SolicitacaoEmprestimoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("solicitacoes")
@Tag(name = "7. Solicitações")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
public class SolicitacaoEmprestimoController {

    @Autowired
    private SolicitacaoEmprestimoService solicitacaoService;

    @GetMapping("/dashboard")
    @Operation(summary = "Lista solicitações pendentes para o dashboard", description = "Retorna uma visão resumida das solicitações aguardando aprovação.")
    public ResponseEntity<List<SolicitacaoDashboardResponse>> listarDashboard() {
        List<SolicitacaoDashboardResponse> pendentes = solicitacaoService.listarSolicitacoesPendentes();
        return ResponseEntity.ok(pendentes);
    }

    @GetMapping("/todas")
    @Operation(summary = "Lista o histórico completo de solicitações", description = "Retorna todas as solicitações registradas, independente do status.")
    public ResponseEntity<List<SolicitacaoCompletaResponse>> listarTodas() {
        return ResponseEntity.ok(solicitacaoService.listarTodasSolicitacoes());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PostMapping("/solicitar")
    @Operation(summary = "Solicita empréstimo de um exemplar específico (Tombo)", description = "Usado quando o usuário sabe exatamente qual exemplar físico deseja.")
    public ResponseEntity<String> solicitar(
            @Parameter(description = "Matrícula do aluno solicitante") @RequestParam String matriculaAluno,
            @Parameter(description = "Código do tombo do exemplar") @RequestParam String tomboExemplar) {
        return solicitacaoService.solicitarEmprestimo(matriculaAluno, tomboExemplar);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PostMapping("/solicitar-mobile")
    @Operation(summary = "Solicita empréstimo via App Mobile (por ID do Livro)", description = "O sistema busca automaticamente o primeiro exemplar disponível deste livro.")
    public ResponseEntity<String> solicitarMobile(
            @Parameter(description = "Matrícula do aluno solicitante") @RequestParam String matriculaAluno,
            @Parameter(description = "ID do livro desejado") @RequestParam Long livroId) {
        return solicitacaoService.solicitarEmprestimoPorLivro(matriculaAluno, livroId);
    }

    @PostMapping("/processar/{id}")
    @Operation(summary = "Processa uma solicitação (Aceitar ou Rejeitar)", description = "Se aceita, gera um empréstimo automaticamente. Se rejeitada, apenas atualiza o status.")
    public ResponseEntity<String> processar(
            @Parameter(description = "ID da solicitação") @PathVariable Integer id,
            @Parameter(description = "True para aceitar, False para rejeitar") @RequestParam boolean aceitar) {
        return solicitacaoService.processarSolicitacao(id, aceitar);
    }

    @GetMapping("/pendentes")
    @Operation(summary = "Lista solicitações com status PENDENTE (DTO detalhado)")
    public List<SolicitacaoResponse> listarPendentes() {
        return solicitacaoService.listarPendentesDTO();
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/aluno/{matricula}")
    @Operation(summary = "Lista solicitações de um aluno específico")
    public List<SolicitacaoResponse> listarDoAluno(
            @Parameter(description = "Matrícula do aluno") @PathVariable String matricula) {
        return solicitacaoService.listarSolicitacoesDoAlunoDTO(matricula);
    }
}