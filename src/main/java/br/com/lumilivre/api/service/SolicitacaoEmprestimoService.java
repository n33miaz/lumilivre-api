package br.com.lumilivre.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoCompletaResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoDashboardResponse;
import br.com.lumilivre.api.dto.solicitacao.SolicitacaoResponse;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.StatusSolicitacao;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.SolicitacaoEmprestimoModel;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.SolicitacaoEmprestimoRepository;
import br.com.lumilivre.api.security.Auditable;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SolicitacaoEmprestimoService {

    private final StudentRepository alunoRepository;
    private final ExemplarRepository exemplarRepository;
    private final SolicitacaoEmprestimoRepository solicitacaoRepository;
    private final EmprestimoService emprestimoService;
    private final EmprestimoRepository emprestimoRepository;
    private final OutboxPublisherService outboxPublisher;

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

    @Cacheable(value = "dashboard_solicitacoes")
    public List<SolicitacaoDashboardResponse> listarSolicitacoesPendentes() {
        return solicitacaoRepository.findSolicitacoesPendentes();
    }

    @Transactional
    @CacheEvict(value = "dashboard_solicitacoes", allEntries = true)
    public ResponseEntity<String> solicitarEmprestimo(String matriculaAluno, String tomboExemplar) {
        Student aluno = alunoRepository.findByMatricula(matriculaAluno).orElse(null);
        if (aluno == null)
            return ResponseEntity.badRequest().body("Aluno não encontrado.");

        ExemplarModel exemplar = exemplarRepository.findByTombo(tomboExemplar).orElse(null);
        if (exemplar == null)
            return ResponseEntity.badRequest().body("Exemplar não encontrado.");

        long ativos = contarEmprestimosAtivos(matriculaAluno);
        RequestApprovalPolicy.validateRequest(aluno.getPenalidadeExpiraEm(), ativos, exemplar.getStatus_livro());

        SolicitacaoEmprestimoModel solicitacao = new SolicitacaoEmprestimoModel();
        solicitacao.setAluno(aluno);
        solicitacao.setExemplar(exemplar);
        solicitacaoRepository.save(solicitacao);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, aluno.getEmail(), "Solicitação recebida",
                "Sua solicitação do livro '" + exemplar.getLivro().getNome() + "' foi registrada.");

        return ResponseEntity.ok("Solicitação registrada com sucesso.");
    }

    @Transactional
    @CacheEvict(value = "dashboard_solicitacoes", allEntries = true)
    public ResponseEntity<String> solicitarEmprestimoPorLivro(String matriculaAluno, Long livroId) {
        Student aluno = alunoRepository.findByMatricula(matriculaAluno).orElse(null);
        if (aluno == null)
            return ResponseEntity.badRequest().body("Aluno não encontrado.");

        ExemplarModel exemplar = exemplarRepository.findFirstDisponivel(livroId, StatusLivro.AVAILABLE).orElse(null);
        if (exemplar == null)
            return ResponseEntity.badRequest().body("Não há exemplares disponíveis para este livro no momento.");

        long ativos = contarEmprestimosAtivos(matriculaAluno);
        RequestApprovalPolicy.validateRequest(aluno.getPenalidadeExpiraEm(), ativos, exemplar.getStatus_livro());

        SolicitacaoEmprestimoModel solicitacao = new SolicitacaoEmprestimoModel();
        solicitacao.setAluno(aluno);
        solicitacao.setExemplar(exemplar);
        solicitacao.setObservacao("Solicitado via Mobile");
        solicitacaoRepository.save(solicitacao);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, aluno.getEmail(), "Solicitação recebida",
                "Sua solicitação do livro '" + exemplar.getLivro().getNome() + "' foi registrada.");

        return ResponseEntity.ok("Solicitação registrada com sucesso.");
    }

    @Auditable(action = "REQUEST_PROCESSED", targetParam = "#id")
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "dashboard_solicitacoes", allEntries = true),
            @CacheEvict(value = "dashboard_stats_emprestimos", allEntries = true),
            @CacheEvict(value = "dashboard_atrasados_list", allEntries = true)
    })
    public ResponseEntity<String> processarSolicitacao(Integer id, boolean aceitar) {
        SolicitacaoEmprestimoModel solicitacao = solicitacaoRepository.findById(id).orElse(null);
        if (solicitacao == null)
            return ResponseEntity.badRequest().body("Solicitação não encontrada.");

        Student aluno = solicitacao.getAluno();
        ExemplarModel exemplar = solicitacao.getExemplar();
        RequestApprovalPolicy.validateProcessable(solicitacao.getStatus());

        if (aceitar) {
            EmprestimoRequest dto = new EmprestimoRequest();
            dto.setAluno_matricula(aluno.getMatricula());
            dto.setExemplar_tombo(exemplar.getTombo());
            dto.setData_emprestimo(LocalDateTime.now());
            dto.setData_devolucao(LocalDateTime.now().plusDays(14));
            emprestimoService.cadastrar(dto);

            solicitacao.setStatus(StatusSolicitacao.ACCEPTED);
            solicitacaoRepository.save(solicitacao);

            outboxPublisher.publish(EventType.REQUEST_ACCEPTED, aluno.getEmail(), "Solicitação aceita",
                    "Sua solicitação do livro '" + exemplar.getLivro().getNome()
                            + "' foi aceita e o empréstimo registrado.");
        } else {
            solicitacao.setStatus(StatusSolicitacao.REJECTED);
            solicitacaoRepository.save(solicitacao);

            outboxPublisher.publish(EventType.REQUEST_REJECTED, aluno.getEmail(), "Solicitação rejeitada",
                    "Sua solicitação do livro '" + exemplar.getLivro().getNome() + "' foi rejeitada.");
        }

        return ResponseEntity.ok("Solicitação processada com sucesso.");
    }

    public List<SolicitacaoResponse> listarPendentesDTO() {
        return solicitacaoRepository.findByStatus(StatusSolicitacao.PENDING)
                .stream()
                .map(s -> new SolicitacaoResponse(
                        s.getId(),
                        s.getAluno().getNomeCompleto(),
                        s.getAluno().getMatricula(),
                        s.getExemplar().getTombo(),
                        s.getExemplar().getLivro().getId(),
                        s.getExemplar().getLivro().getNome(),
                        s.getDataSolicitacao(),
                        s.getStatus(),
                        s.getObservacao()))
                .toList();
    }

    public List<SolicitacaoResponse> listarSolicitacoesDoAlunoDTO(String matricula) {
        return solicitacaoRepository.findByAlunoMatriculaOrderByDataSolicitacaoDesc(matricula)
                .stream()
                .map(s -> new SolicitacaoResponse(
                        s.getId(),
                        s.getAluno().getNomeCompleto(),
                        s.getAluno().getMatricula(),
                        s.getExemplar().getTombo(),
                        s.getExemplar().getLivro().getId(),
                        s.getExemplar().getLivro().getNome(),
                        s.getDataSolicitacao(),
                        s.getStatus(),
                        s.getObservacao()))
                .toList();
    }

    private long contarEmprestimosAtivos(String matricula) {
        return emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo(matricula, StatusEmprestimo.ACTIVE)
                + emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo(matricula, StatusEmprestimo.OVERDUE);
    }
}
