package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.enums.StatusReserva;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.ReservaModel;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import br.com.lumilivre.api.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private StudentRepository alunoRepository;

    @Mock
    private LivroRepository livroRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @InjectMocks
    private ReservaService service;

    @Test
    void criarReservaDeveSalvarProximaPosicaoDaFilaEPublicarOutbox() {
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno("12345")));
        when(livroRepository.findById(10L)).thenReturn(Optional.of(livro()));
        when(reservaRepository.findByAlunoMatriculaOrderByCriadaEmDesc("12345")).thenReturn(List.of());
        when(reservaRepository.existsByAlunoMatriculaAndLivroIdAndStatusIn(eq("12345"), eq(10L), any()))
                .thenReturn(false);
        when(reservaRepository.maxPosicaoFila(10L)).thenReturn(2);
        when(reservaRepository.save(any(ReservaModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaModel reserva = service.criarReserva("12345", 10L);

        assertThat(reserva.getAluno().getMatricula()).isEqualTo("12345");
        assertThat(reserva.getLivro().getId()).isEqualTo(10L);
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.WAITING);
        assertThat(reserva.getPosicaoFila()).isEqualTo(3);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                eq("Reserva registrada"),
                org.mockito.ArgumentMatchers.contains("3"));
    }

    @Test
    void criarReservaDeveBloquearReservaDuplicadaAtiva() {
        when(alunoRepository.findByMatricula("12345")).thenReturn(Optional.of(aluno("12345")));
        when(livroRepository.findById(10L)).thenReturn(Optional.of(livro()));
        when(reservaRepository.findByAlunoMatriculaOrderByCriadaEmDesc("12345")).thenReturn(List.of());
        when(reservaRepository.existsByAlunoMatriculaAndLivroIdAndStatusIn(eq("12345"), eq(10L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.criarReserva("12345", 10L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("reserva ativa");

        verify(reservaRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void cancelarReservaDeveExigirAlunoDonoDaReserva() {
        ReservaModel reserva = reserva("12345", StatusReserva.WAITING);
        when(reservaRepository.findById(7L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.cancelarReserva(7L, "99999"))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelarReservaDeveMarcarComoCanceladaQuandoAlunoEDono() {
        ReservaModel reserva = reserva("12345", StatusReserva.WAITING);
        when(reservaRepository.findById(7L)).thenReturn(Optional.of(reserva));

        service.cancelarReserva(7L, "12345");

        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CANCELLED);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void notificarProximoDaFilaDeveDisponibilizarReservaEPublicarOutbox() {
        ReservaModel reserva = reserva("12345", StatusReserva.WAITING);
        when(reservaRepository.findFirstByLivroIdAndStatusOrderByPosicaoFilaAsc(10L, StatusReserva.WAITING))
                .thenReturn(Optional.of(reserva));

        service.notificarProximoDaFila(10L);

        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.READY);
        assertThat(reserva.getNotificadoEm()).isNotNull();
        assertThat(reserva.getExpiraEm()).isAfter(reserva.getNotificadoEm());
        verify(reservaRepository).save(reserva);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("dispon"),
                org.mockito.ArgumentMatchers.contains("Livro Teste"));
    }

    @Test
    void expirarReservasVencidasDeveMarcarExpiradaENotificarProximo() {
        ReservaModel vencida = reserva("12345", StatusReserva.READY);
        vencida.setExpiraEm(LocalDateTime.now().minusDays(1));

        when(reservaRepository.findByStatusAndExpiraEmBefore(eq(StatusReserva.READY), any()))
                .thenReturn(List.of(vencida));
        when(reservaRepository.findFirstByLivroIdAndStatusOrderByPosicaoFilaAsc(10L, StatusReserva.WAITING))
                .thenReturn(Optional.empty());

        service.expirarReservasVencidas();

        assertThat(vencida.getStatus()).isEqualTo(StatusReserva.EXPIRED);
        verify(reservaRepository).save(vencida);
    }

    private static ReservaModel reserva(String matricula, StatusReserva status) {
        ReservaModel reserva = new ReservaModel();
        reserva.setId(7L);
        reserva.setAluno(aluno(matricula));
        reserva.setLivro(livro());
        reserva.setStatus(status);
        reserva.setPosicaoFila(1);
        return reserva;
    }

    private static Student aluno(String matricula) {
        Student aluno = new Student();
        aluno.setMatricula(matricula);
        aluno.setNomeCompleto("Aluno Teste");
        aluno.setEmail("aluno@lumilivre.test");
        return aluno;
    }

    private static LivroModel livro() {
        LivroModel livro = new LivroModel();
        livro.setId(10L);
        livro.setNome("Livro Teste");
        return livro;
    }
}
