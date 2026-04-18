package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy.RequestApprovalViolationException;
import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.StatusSolicitacao;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.OutboxEventModel.EventType;
import br.com.lumilivre.api.model.SolicitacaoEmprestimoModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.SolicitacaoEmprestimoRepository;

@ExtendWith(MockitoExtension.class)
class SolicitacaoEmprestimoServiceTest {

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private ExemplarRepository exemplarRepository;

    @Mock
    private SolicitacaoEmprestimoRepository solicitacaoRepository;

    @Mock
    private EmprestimoService emprestimoService;

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @InjectMocks
    private SolicitacaoEmprestimoService service;

    @Test
    void solicitarEmprestimoDeveRetornarBadRequestQuandoAlunoNaoExiste() {
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.empty());

        var response = service.solicitarEmprestimo("12345", "T001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Aluno");
        verify(solicitacaoRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void solicitarEmprestimoDeveRetornarBadRequestQuandoExemplarNaoExiste() {
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno()));
        when(exemplarRepository.findByTombo("T001")).thenReturn(Optional.empty());

        var response = service.solicitarEmprestimo("12345", "T001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Exemplar");
        verify(solicitacaoRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void solicitarEmprestimoDeveRegistrarSolicitacaoPendenteEPublicarOutbox() {
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno()));
        when(exemplarRepository.findByTombo("T001")).thenReturn(Optional.of(exemplar(StatusLivro.DISPONIVEL)));
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo("12345", StatusEmprestimo.ATIVO))
                .thenReturn(1L);
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo("12345", StatusEmprestimo.ATRASADO))
                .thenReturn(0L);

        var response = service.solicitarEmprestimo("12345", "T001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<SolicitacaoEmprestimoModel> captor =
                ArgumentCaptor.forClass(SolicitacaoEmprestimoModel.class);
        verify(solicitacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getAluno().getMatricula()).isEqualTo("12345");
        assertThat(captor.getValue().getExemplar().getTombo()).isEqualTo("T001");
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusSolicitacao.PENDENTE);

        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("recebida"),
                org.mockito.ArgumentMatchers.contains("Livro Teste"));
    }

    @Test
    void solicitarEmprestimoPorLivroDeveUsarPrimeiroExemplarDisponivelERegistrarOrigemMobile() {
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno()));
        when(exemplarRepository.findFirstDisponivel(10L, StatusLivro.DISPONIVEL))
                .thenReturn(Optional.of(exemplar(StatusLivro.DISPONIVEL)));
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo("12345", StatusEmprestimo.ATIVO))
                .thenReturn(0L);
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo("12345", StatusEmprestimo.ATRASADO))
                .thenReturn(0L);

        var response = service.solicitarEmprestimoPorLivro("12345", 10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<SolicitacaoEmprestimoModel> captor =
                ArgumentCaptor.forClass(SolicitacaoEmprestimoModel.class);
        verify(solicitacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getObservacao()).isEqualTo("Solicitado via Mobile");
        assertThat(captor.getValue().getExemplar().getTombo()).isEqualTo("T001");
    }

    @Test
    void solicitarEmprestimoDeveBloquearExemplarIndisponivel() {
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno()));
        when(exemplarRepository.findByTombo("T001")).thenReturn(Optional.of(exemplar(StatusLivro.EMPRESTADO)));
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo("12345", StatusEmprestimo.ATIVO))
                .thenReturn(0L);
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo("12345", StatusEmprestimo.ATRASADO))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.solicitarEmprestimo("12345", "T001"))
                .isInstanceOf(BookAvailabilityViolationException.class);

        verify(solicitacaoRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void processarSolicitacaoAceitaDeveCadastrarEmprestimoAtualizarStatusEPublicarOutbox() {
        SolicitacaoEmprestimoModel solicitacao = solicitacao(StatusSolicitacao.PENDENTE);
        when(solicitacaoRepository.findById(7)).thenReturn(Optional.of(solicitacao));

        var response = service.processarSolicitacao(7, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.ACEITA);

        ArgumentCaptor<EmprestimoRequest> requestCaptor = ArgumentCaptor.forClass(EmprestimoRequest.class);
        verify(emprestimoService).cadastrar(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getAluno_matricula()).isEqualTo("12345");
        assertThat(requestCaptor.getValue().getExemplar_tombo()).isEqualTo("T001");
        assertThat(requestCaptor.getValue().getData_devolucao())
                .isAfter(requestCaptor.getValue().getData_emprestimo());

        verify(solicitacaoRepository).save(solicitacao);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("aceita"),
                org.mockito.ArgumentMatchers.contains("aceita"));
    }

    @Test
    void processarSolicitacaoRejeitadaDeveAtualizarStatusSemCadastrarEmprestimo() {
        SolicitacaoEmprestimoModel solicitacao = solicitacao(StatusSolicitacao.PENDENTE);
        when(solicitacaoRepository.findById(7)).thenReturn(Optional.of(solicitacao));

        var response = service.processarSolicitacao(7, false);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.REJEITADA);
        verify(emprestimoService, never()).cadastrar(any());
        verify(solicitacaoRepository).save(solicitacao);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_REJECTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("rejeitada"),
                org.mockito.ArgumentMatchers.contains("rejeitada"));
    }

    @Test
    void processarSolicitacaoDeveRetornarBadRequestQuandoNaoExiste() {
        when(solicitacaoRepository.findById(7)).thenReturn(Optional.empty());

        var response = service.processarSolicitacao(7, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(emprestimoService, never()).cadastrar(any());
        verify(solicitacaoRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void processarSolicitacaoDeveBloquearSolicitacaoJaProcessada() {
        SolicitacaoEmprestimoModel solicitacao = solicitacao(StatusSolicitacao.ACEITA);
        when(solicitacaoRepository.findById(7)).thenReturn(Optional.of(solicitacao));

        assertThatThrownBy(() -> service.processarSolicitacao(7, false))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessageContaining("pendente");

        verify(emprestimoService, never()).cadastrar(any());
        verify(solicitacaoRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    private static SolicitacaoEmprestimoModel solicitacao(StatusSolicitacao status) {
        SolicitacaoEmprestimoModel solicitacao = new SolicitacaoEmprestimoModel();
        solicitacao.setId(7);
        solicitacao.setAluno(aluno());
        solicitacao.setExemplar(exemplar(StatusLivro.DISPONIVEL));
        solicitacao.setStatus(status);
        return solicitacao;
    }

    private static AlunoModel aluno() {
        AlunoModel aluno = new AlunoModel();
        aluno.setMatricula("12345");
        aluno.setNomeCompleto("Aluno Teste");
        aluno.setEmail("aluno@lumilivre.test");
        return aluno;
    }

    private static ExemplarModel exemplar(StatusLivro status) {
        LivroModel livro = new LivroModel();
        livro.setId(10L);
        livro.setNome("Livro Teste");

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo("T001");
        exemplar.setStatus_livro(status);
        exemplar.setLivro(livro);
        return exemplar;
    }
}
