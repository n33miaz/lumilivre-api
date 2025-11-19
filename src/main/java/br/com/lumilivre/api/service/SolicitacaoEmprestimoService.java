package br.com.lumilivre.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoCompletaResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoResponse;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.StatusSolicitacao;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.SolicitacaoEmprestimoModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.SolicitacaoEmprestimoRepository;
import jakarta.transaction.Transactional;

@Service
public class SolicitacaoEmprestimoService {

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private ExemplarRepository exemplarRepository;

    @Autowired
    private SolicitacaoEmprestimoRepository solicitacaoRepository;

    @Autowired
    private EmprestimoService emprestimoService;

    @Autowired
    private EmailService emailService;

    private static final int LIMITE_EMPRESTIMOS_ATIVOS = 3;

    public List<SolicitacaoCompletaResponse> listarTodasSolicitacoes() {
        return solicitacaoRepository.findAllByOrderByDataSolicitacaoDesc()
                .stream()
                .map(s -> new SolicitacaoCompletaResponse(
                        s.getId(),
                        s.getAluno().getNomeCompleto(),
                        s.getAluno().getMatricula(),
                        s.getExemplar().getTombo(),
                        s.getExemplar().getLivro().getNome(),
                        s.getDataSolicitacao(),
                        s.getStatus(),
                        s.getObservacao()))
                .toList();
    }

    public List<SolicitacaoDashboardResponse> listarSolicitacoesPendentes() {
        return solicitacaoRepository.findSolicitacoesPendentes();
    }

    // Aluno solicita empréstimo
    @Transactional
    public ResponseEntity<String> solicitarEmprestimo(String matriculaAluno, String tomboExemplar) {
        AlunoModel aluno = alunoRepository.findByMatricula(matriculaAluno).orElse(null);
        if (aluno == null)
            return ResponseEntity.badRequest().body("Aluno não encontrado.");
        if (aluno.getPenalidade() != null)
            return ResponseEntity.badRequest().body("Aluno possui penalidade ativa.");

        long emprestimosAtivos = emprestimoService.getContagemEmprestimosAtivosEAtrasados();
        if (emprestimosAtivos >= LIMITE_EMPRESTIMOS_ATIVOS)
            return ResponseEntity.badRequest().body("Aluno atingiu limite de empréstimos ativos.");

        ExemplarModel exemplar = exemplarRepository.findByTombo(tomboExemplar).orElse(null);
        if (exemplar == null)
            return ResponseEntity.badRequest().body("Exemplar não encontrado.");
        if (exemplar.getStatus_livro() != StatusLivro.DISPONIVEL)
            return ResponseEntity.badRequest().body("Exemplar indisponível.");

        SolicitacaoEmprestimoModel solicitacao = new SolicitacaoEmprestimoModel();
        solicitacao.setAluno(aluno);
        solicitacao.setExemplar(exemplar);
        solicitacaoRepository.save(solicitacao);

        emailService.enviarEmail(aluno.getEmail(), "Solicitação recebida",
                "Sua solicitação do livro '" + exemplar.getLivro().getNome() + "' foi registrada.");

        return ResponseEntity.ok("Solicitação registrada com sucesso.");
    }

    // Biblioteca processa a solicitação
    @Transactional
    public ResponseEntity<String> processarSolicitacao(Integer id, boolean aceitar) {
        SolicitacaoEmprestimoModel solicitacao = solicitacaoRepository.findById(id).orElse(null);
        if (solicitacao == null)
            return ResponseEntity.badRequest().body("Solicitação não encontrada.");

        AlunoModel aluno = solicitacao.getAluno();
        ExemplarModel exemplar = solicitacao.getExemplar();

        if (aceitar) {
            // Cria empréstimo automaticamente
            EmprestimoRequest dto = new EmprestimoRequest();
            dto.setAluno_matricula(aluno.getMatricula());
            dto.setExemplar_tombo(exemplar.getTombo());
            dto.setData_emprestimo(LocalDateTime.now());
            dto.setData_devolucao(LocalDateTime.now().plusDays(14)); // padrão 7 dias

            emprestimoService.cadastrar(dto);

            solicitacao.setStatus(StatusSolicitacao.ACEITA);
            solicitacaoRepository.save(solicitacao);

            emailService.enviarEmail(aluno.getEmail(), "Solicitação aceita",
                    "Sua solicitação do livro '" + exemplar.getLivro().getNome()
                            + "' foi aceita e o empréstimo registrado.");
        } else {
            solicitacao.setStatus(StatusSolicitacao.REJEITADA);
            solicitacaoRepository.save(solicitacao);

            emailService.enviarEmail(aluno.getEmail(), "Solicitação rejeitada",
                    "Sua solicitação do livro '" + exemplar.getLivro().getNome() + "' foi rejeitada.");
        }

        return ResponseEntity.ok("Solicitação processada com sucesso.");
    }

    public List<SolicitacaoResponse> listarPendentesDTO() {
        return solicitacaoRepository.findByStatus(StatusSolicitacao.PENDENTE)
                .stream()
                .map(s -> new SolicitacaoResponse(
                        s.getId(),
                        s.getAluno().getNomeCompleto(),
                        s.getAluno().getMatricula(),
                        s.getExemplar().getTombo(),
                        s.getExemplar().getLivro().getNome(),
                        s.getDataSolicitacao(),
                        s.getStatus(),
                        s.getObservacao()))
                .toList();
    }

    public List<SolicitacaoResponse> listarSolicitacoesDoAlunoDTO(String matricula) {
        return solicitacaoRepository.findByAlunoMatriculaAndStatus(matricula, StatusSolicitacao.PENDENTE)
                .stream()
                .map(s -> new SolicitacaoResponse(
                        s.getId(),
                        s.getAluno().getNomeCompleto(),
                        s.getAluno().getMatricula(),
                        s.getExemplar().getTombo(),
                        s.getExemplar().getLivro().getNome(),
                        s.getDataSolicitacao(),
                        s.getStatus(),
                        s.getObservacao()))
                .toList();
    }

}
