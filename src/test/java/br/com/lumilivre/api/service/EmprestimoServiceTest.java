package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.dto.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.LoanPolicy.LoanPolicyViolationException;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class EmprestimoServiceTest {

    @Mock
    private AlunoRepository alunoRepository;

    @Mock
    private ExemplarRepository exemplarRepository;

    @Mock
    private EmprestimoRepository emprestimoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @InjectMocks
    private EmprestimoService service;

    @Test
    void cadastrarDeveBloquearDataDevolucaoAnteriorAoEmprestimo() {
        EmprestimoRequest request = request();
        request.setData_devolucao(request.getData_emprestimo().minusDays(1));

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("devolu");

        verify(emprestimoRepository, never()).save(any(EmprestimoModel.class));
    }

    @Test
    void cadastrarDeveBloquearAlunoComLimiteDeEmprestimosAtivos() {
        EmprestimoRequest request = request();
        AlunoModel aluno = aluno();
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno));
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo(any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(LoanPolicyViolationException.class);

        verify(exemplarRepository, never()).findByTombo(any());
        verify(emprestimoRepository, never()).save(any(EmprestimoModel.class));
    }

    @Test
    void cadastrarDeveBloquearAlunoComPenalidadeAtiva() {
        EmprestimoRequest request = request();
        AlunoModel aluno = aluno();
        aluno.setPenalidadeExpiraEm(LocalDateTime.now().plusDays(2));
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno));
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo(any(), any())).thenReturn(0L);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(LoanPolicyViolationException.class);

        verify(exemplarRepository, never()).findByTombo(any());
        verify(emprestimoRepository, never()).save(any(EmprestimoModel.class));
    }

    @Test
    void cadastrarDeveBloquearExemplarIndisponivel() {
        EmprestimoRequest request = request();
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno()));
        when(emprestimoRepository.countByAlunoMatriculaAndStatusEmprestimo(any(), any())).thenReturn(0L);
        when(exemplarRepository.findByTombo("T001")).thenReturn(Optional.of(exemplar(StatusLivro.BORROWED)));

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(BookAvailabilityViolationException.class);

        verify(emprestimoRepository, never()).save(any(EmprestimoModel.class));
    }

    private static EmprestimoRequest request() {
        LocalDateTime now = LocalDateTime.now();
        return EmprestimoRequest.builder()
                .aluno_matricula("12345")
                .exemplar_tombo("T001")
                .data_emprestimo(now)
                .data_devolucao(now.plusDays(14))
                .build();
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
        livro.setId(1L);
        livro.setNome("Livro Teste");

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo("T001");
        exemplar.setStatus_livro(status);
        exemplar.setLivro(livro);
        return exemplar;
    }
}
