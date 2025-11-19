package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.dto.ListaSolicitacaoCompletaDTO;
import br.com.lumilivre.api.dto.ListaSolicitacaoDashboardDTO;
import br.com.lumilivre.api.dto.SolicitacaoEmprestimoDTO;
import br.com.lumilivre.api.service.SolicitacaoEmprestimoService;

@RestController
@RequestMapping("solicitacoes")
@PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
public class SolicitacaoEmprestimoController {

    @Autowired
    private SolicitacaoEmprestimoService solicitacaoService;

    @GetMapping("/dashboard")
    public ResponseEntity<List<ListaSolicitacaoDashboardDTO>> listarDashboard() {
        List<ListaSolicitacaoDashboardDTO> pendentes = solicitacaoService.listarSolicitacoesPendentes();
        return ResponseEntity.ok(pendentes);
    }

    @GetMapping("/todas")
    public ResponseEntity<List<ListaSolicitacaoCompletaDTO>> listarTodas() {
        return ResponseEntity.ok(solicitacaoService.listarTodasSolicitacoes());
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @PostMapping("/solicitar")
    public ResponseEntity<String> solicitar(@RequestParam String matriculaAluno,
            @RequestParam String tomboExemplar) {
        return solicitacaoService.solicitarEmprestimo(matriculaAluno, tomboExemplar);
    }

    @PostMapping("/processar/{id}")
    public ResponseEntity<String> processar(@PathVariable Integer id,
            @RequestParam boolean aceitar) {
        return solicitacaoService.processarSolicitacao(id, aceitar);
    }

    @GetMapping("/pendentes")
    public List<SolicitacaoEmprestimoDTO> listarPendentes() {
        return solicitacaoService.listarPendentesDTO();
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/aluno/{matricula}")
    public List<SolicitacaoEmprestimoDTO> listarDoAluno(@PathVariable String matricula) {
        return solicitacaoService.listarSolicitacoesDoAlunoDTO(matricula);
    }

}
