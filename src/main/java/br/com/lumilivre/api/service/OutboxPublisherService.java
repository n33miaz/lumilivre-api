package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

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
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("pt-BR");

    private final OutboxEventRepository outboxRepository;
    private final EmailService emailService;

    /**
     * Persiste um evento no outbox usando o locale padrão (pt-BR).
     * Mantido por compatibilidade; prefira a sobrecarga com {@link Locale}.
     */
    @Transactional
    public void publish(EventType type, String recipientEmail, String subject, String body) {
        publish(type, recipientEmail, subject, body, DEFAULT_LOCALE);
    }

    /**
     * Persiste um evento no outbox dentro da transação do chamador, registrando o
     * locale do destinatário para que o envio assíncrono renderize o shell do
     * e-mail no mesmo idioma do assunto/corpo já resolvidos.
     */
    @Transactional
    public void publish(EventType type, String recipientEmail, String subject, String body, Locale locale) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(type)
                .recipientEmail(recipientEmail)
                .subject(subject)
                .body(body)
                .locale(locale != null ? locale.toLanguageTag() : DEFAULT_LOCALE.toLanguageTag())
                .build();
        outboxRepository.save(event);
        log.debug("Outbox event persisted: type={}, recipient={}, locale={}", type, recipientEmail, event.getLocale());
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
                dispatch(event);
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

    /**
     * Escolhe o template pelo tipo. A recuperação de senha precisa do template
     * dedicado — o genérico escapa o corpo e põe um botão fixo para o portal, o
     * que transformaria o link de redefinição em texto não clicável.
     */
    private void dispatch(OutboxEvent event) {
        Locale locale = parseLocale(event.getLocale());
        if (event.getEventType() == EventType.PASSWORD_RESET) {
            emailService.enviarEmailResetSenha(event.getRecipientEmail(), event.getBody(), locale);
            return;
        }
        emailService.enviarEmail(event.getRecipientEmail(), event.getSubject(), event.getBody(), locale);
    }

    private static Locale parseLocale(String tag) {
        return (tag != null && !tag.isBlank()) ? Locale.forLanguageTag(tag) : DEFAULT_LOCALE;
    }
}
