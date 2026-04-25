package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.model.OutboxEvent;
import br.com.lumilivre.api.model.OutboxEvent.EventStatus;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.repository.OutboxEventRepository;
import br.com.lumilivre.api.service.infra.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);
    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxRepository;
    private final EmailService emailService;

    /**
     * Persiste um evento no outbox dentro da transação do chamador.
     * O email é enviado de forma assíncrona pelo scheduler.
     */
    @Transactional
    public void publish(EventType type, String recipientEmail, String subject, String body) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(type)
                .recipientEmail(recipientEmail)
                .subject(subject)
                .body(body)
                .build();
        outboxRepository.save(event);
        log.debug("Outbox event persisted: type={}, recipient={}", type, recipientEmail);
    }

    /**
     * Processa eventos PENDING a cada 30 segundos.
     * Falha de SMTP não reverte a transação principal.
     */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pending = outboxRepository
                .findByStatusAndRetryCountLessThan(EventStatus.PENDING, MAX_RETRIES);

        for (OutboxEvent event : pending) {
            try {
                emailService.enviarEmail(event.getRecipientEmail(), event.getSubject(), event.getBody());
                event.setStatus(EventStatus.SENT);
                event.setProcessedAt(OffsetDateTime.now());
                log.info("Outbox email sent: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setNextRetryAt(OffsetDateTime.now().plusMinutes(5L * event.getRetryCount()));

                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(EventStatus.FAILED);
                    log.error("Outbox event failed after {} retries: id={}, recipient={}",
                            MAX_RETRIES, event.getId(), event.getRecipientEmail());
                } else {
                    log.warn("Outbox email retry {}/{}: id={}, error={}",
                            event.getRetryCount(), MAX_RETRIES, event.getId(), e.getMessage());
                }
            }
            outboxRepository.save(event);
        }
    }
}
