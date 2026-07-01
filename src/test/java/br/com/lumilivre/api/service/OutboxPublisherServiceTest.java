package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.model.OutboxEvent;
import br.com.lumilivre.api.model.OutboxEvent.EventStatus;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.repository.OutboxEventRepository;
import br.com.lumilivre.api.service.infra.EmailService;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OutboxPublisherService service;

    @Test
    void publishDevePersistirEventoPendente() {
        service.publish(EventType.LOAN_CREATED, "leitor@lumilivre.test", "Emprestimo criado", "Corpo");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(EventType.LOAN_CREATED);
        assertThat(event.getRecipientEmail()).isEqualTo("leitor@lumilivre.test");
        assertThat(event.getSubject()).isEqualTo("Emprestimo criado");
        assertThat(event.getBody()).isEqualTo("Corpo");
        assertThat(event.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void processPendingEventsDeveEnviarEmailEMarcarComoEnviado() {
        OutboxEvent event = event(0);
        when(outboxRepository.findByStatusAndRetryCountLessThan(EventStatus.PENDING, 3))
                .thenReturn(List.of(event));

        service.processPendingEvents();

        verify(emailService).enviarEmail(eq("leitor@lumilivre.test"), eq("Assunto"), eq("Corpo"), any());
        assertThat(event.getStatus()).isEqualTo(EventStatus.SENT);
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getRetryCount()).isZero();
        verify(outboxRepository).save(event);
    }

    @Test
    void processPendingEventsDeveAgendarRetryQuandoEnvioFalhaAntesDoLimite() {
        OutboxEvent event = event(1);
        when(outboxRepository.findByStatusAndRetryCountLessThan(EventStatus.PENDING, 3))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("smtp indisponivel"))
                .when(emailService).enviarEmail(any(), any(), any(), any());

        service.processPendingEvents();

        assertThat(event.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(2);
        assertThat(event.getNextRetryAt()).isNotNull();
        assertThat(event.getProcessedAt()).isNull();
        verify(outboxRepository).save(event);
    }

    @Test
    void processPendingEventsDeveMarcarComoFalhaAoAtingirLimiteDeRetries() {
        OutboxEvent event = event(2);
        when(outboxRepository.findByStatusAndRetryCountLessThan(EventStatus.PENDING, 3))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("smtp indisponivel"))
                .when(emailService).enviarEmail(any(), any(), any(), any());

        service.processPendingEvents();

        assertThat(event.getStatus()).isEqualTo(EventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getNextRetryAt()).isNotNull();
        verify(outboxRepository).save(event);
    }

    private static OutboxEvent event(int retryCount) {
        return OutboxEvent.builder()
                .id(10L)
                .eventType(EventType.LOAN_CREATED)
                .recipientEmail("leitor@lumilivre.test")
                .subject("Assunto")
                .body("Corpo")
                .status(EventStatus.PENDING)
                .retryCount(retryCount)
                .build();
    }
}
