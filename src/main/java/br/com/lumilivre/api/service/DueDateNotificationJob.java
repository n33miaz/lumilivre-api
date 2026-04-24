package br.com.lumilivre.api.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Envia lembretes de devolução via Outbox para empréstimos ATIVOS:
 *   D-3 e D-1 antes do vencimento, D0 (dia do vencimento), e notificação de atraso.
 * Executa diariamente às 08:00.
 */
@Service
@RequiredArgsConstructor
public class DueDateNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(DueDateNotificationJob.class);

    private final EmprestimoRepository emprestimoRepository;
    private final OutboxPublisherService outboxPublisher;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void enviarLembretes() {
        LocalDateTime agora = LocalDateTime.now();

        notificarWindowDias(agora, 3, "Lembrete: devolução em 3 dias",
                "Você tem 3 dias para devolver o livro '%s'.");
        notificarWindowDias(agora, 1, "Lembrete: devolução amanhã",
                "Seu empréstimo do livro '%s' vence amanhã.");
        notificarWindowDias(agora, 0, "Devolução hoje",
                "O prazo de devolução do livro '%s' é hoje.");
        notificarAtrasados(agora);
    }

    private void notificarWindowDias(LocalDateTime agora, int diasRestantes, String assunto, String template) {
        LocalDateTime inicio = agora.plusDays(diasRestantes).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime fim = inicio.plusDays(1);

        List<EmprestimoModel> vencendo = emprestimoRepository
                .findByStatusEmprestimoAndDataDevolucaoGreaterThanEqual(StatusEmprestimo.ACTIVE, inicio)
                .stream()
                .filter(e -> e.getDataDevolucao().isBefore(fim))
                .toList();

        for (EmprestimoModel e : vencendo) {
            String nomeLivro = e.getExemplar().getLivro().getNome();
            outboxPublisher.publish(EventType.REQUEST_ACCEPTED,
                    e.getAluno().getEmail(),
                    assunto,
                    String.format(template, nomeLivro));
        }

        if (!vencendo.isEmpty()) {
            log.info("DueDateNotificationJob: {} lembrete(s) D-{} enviado(s).", vencendo.size(), diasRestantes);
        }
    }

    private void notificarAtrasados(LocalDateTime agora) {
        List<EmprestimoModel> atrasados = emprestimoRepository
                .findByStatusEmprestimo(StatusEmprestimo.OVERDUE);

        for (EmprestimoModel e : atrasados) {
            String nomeLivro = e.getExemplar().getLivro().getNome();
            outboxPublisher.publish(EventType.REQUEST_REJECTED,
                    e.getAluno().getEmail(),
                    "Empréstimo em atraso",
                    "Seu empréstimo do livro '" + nomeLivro + "' está em atraso. Regularize o quanto antes.");
        }

        if (!atrasados.isEmpty()) {
            log.info("DueDateNotificationJob: {} notificação(ões) de atraso enviada(s).", atrasados.size());
        }
    }
}
