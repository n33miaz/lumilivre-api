package br.com.lumilivre.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.domain.policy.ReservationPolicy;
import br.com.lumilivre.api.enums.StatusReserva;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.OutboxEventModel.EventType;
import br.com.lumilivre.api.model.ReservaModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import br.com.lumilivre.api.repository.ReservaRepository;
import br.com.lumilivre.api.security.Auditable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final AlunoRepository alunoRepository;
    private final LivroRepository livroRepository;
    private final OutboxPublisherService outboxPublisher;

    @Auditable(action = "RESERVATION_CREATED", targetParam = "#matricula")
    @Transactional
    public ReservaModel criarReserva(String matricula, Long livroId) {
        AlunoModel aluno = alunoRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        LivroModel livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado."));

        long activeReservations = reservaRepository
                .findByAlunoMatriculaOrderByCriadaEmDesc(matricula)
                .stream()
                .filter(r -> ReservationPolicy.activeStatuses().contains(r.getStatus()))
                .count();

        boolean alreadyReserved = reservaRepository.existsByAlunoMatriculaAndLivroIdAndStatusIn(
                matricula, livroId, ReservationPolicy.activeStatuses());

        ReservationPolicy.validateNewReservation(
                aluno.getPenalidadeExpiraEm(), activeReservations, alreadyReserved);

        int proxima = reservaRepository.maxPosicaoFila(livroId) + 1;

        ReservaModel reserva = new ReservaModel();
        reserva.setAluno(aluno);
        reserva.setLivro(livro);
        reserva.setPosicaoFila(proxima);

        ReservaModel salva = reservaRepository.save(reserva);

        outboxPublisher.publish(EventType.REQUEST_ACCEPTED, aluno.getEmail(),
                "Reserva registrada",
                "Sua reserva do livro '" + livro.getNome() + "' foi registrada (posição " + proxima + " na fila).");

        return salva;
    }

    @Auditable(action = "RESERVATION_CANCELLED", targetParam = "#reservaId")
    @Transactional
    public void cancelarReserva(Long reservaId, String matriculaCaller) {
        ReservaModel reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));

        if (!reserva.getAluno().getMatricula().equals(matriculaCaller)) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada.");
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        reservaRepository.save(reserva);
    }

    /**
     * Invocado quando um empréstimo é concluído (devolução).
     * Notifica o próximo da fila FIFO para o livro.
     */
    @Transactional
    public void notificarProximoDaFila(Long livroId) {
        reservaRepository.findFirstByLivroIdAndStatusOrderByPosicaoFilaAsc(
                livroId, StatusReserva.AGUARDANDO)
                .ifPresent(proxima -> {
                    LocalDateTime agora = LocalDateTime.now();
                    proxima.setStatus(StatusReserva.DISPONIVEL_PARA_RETIRADA);
                    proxima.setNotificadoEm(agora);
                    proxima.setExpiraEm(ReservationPolicy.calculatePickupDeadline(agora));
                    reservaRepository.save(proxima);

                    outboxPublisher.publish(EventType.REQUEST_ACCEPTED,
                            proxima.getAluno().getEmail(),
                            "Livro disponível para retirada",
                            "O livro '" + proxima.getLivro().getNome() +
                            "' está disponível. Retire até " +
                            proxima.getExpiraEm().toLocalDate() + ".");
                });
    }

    /** Expira reservas DISPONIVEL_PARA_RETIRADA com prazo vencido. Executa diariamente às 04:00. */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void expirarReservasVencidas() {
        List<ReservaModel> vencidas = reservaRepository
                .findByStatusAndExpiraEmBefore(StatusReserva.DISPONIVEL_PARA_RETIRADA, LocalDateTime.now());

        if (vencidas.isEmpty()) return;

        for (ReservaModel r : vencidas) {
            r.setStatus(StatusReserva.EXPIRADA);
            reservaRepository.save(r);
            notificarProximoDaFila(r.getLivro().getId());
        }

        log.info("ReservaService: {} reserva(s) expirada(s).", vencidas.size());
    }

}
