package br.com.lumilivre.api.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoCompletaResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoResponse;
import br.com.lumilivre.api.service.SolicitacaoEmprestimoService;

@RestController
@RequestMapping("solicitacoes")
@PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
public class SolicitacaoEmprestimoController {

    @Autowired
    private SolicitacaoEmprestimoService solicitacaoService;

    @GetMapping("/dashboard")
    public ResponseEntity<List<SolicitacaoDashboardResponse>> listarDashboard() {
        List<SolicitacaoDashboardResponse> pendentes = solicitacaoService.listarSolicitacoesPendentes();
        return ResponseEntity.ok(pendentes);
    }

    @GetMapping("/todas")
    public ResponseEntity<List<SolicitacaoCompletaResponse>> listarTodas() {
        return ResponseEntity.ok(solicitacaoService.listarTodasSolicitacoes());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PostMapping("/solicitar")
    public ResponseEntity<String> solicitar(@RequestParam String matriculaAluno,
            @RequestParam String tomboExemplar) {
        return solicitacaoService.solicitarEmprestimo(matriculaAluno, tomboExemplar);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PostMapping("/solicitar-mobile")
    public ResponseEntity<String> solicitarMobile(
            @RequestParam String matriculaAluno,
            @RequestParam Long livroId) {
        return solicitacaoService.solicitarEmprestimoPorLivro(matriculaAluno, livroId);
    }

    @PostMapping("/processar/{id}")
    public ResponseEntity<String> processar(@PathVariable Integer id,
            @RequestParam boolean aceitar) {
        return solicitacaoService.processarSolicitacao(id, aceitar);
    }

    @GetMapping("/pendentes")
    public List<SolicitacaoResponse> listarPendentes() {
        return solicitacaoService.listarPendentesDTO();
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/aluno/{matricula}")
    public List<SolicitacaoResponse> listarDoAluno(@PathVariable String matricula) {
        return solicitacaoService.listarSolicitacoesDoAlunoDTO(matricula);
    }

}
