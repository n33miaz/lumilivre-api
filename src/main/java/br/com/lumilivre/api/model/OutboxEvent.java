package br.com.lumilivre.api.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    public enum EventType {
        LOAN_CREATED,
        LOAN_RETURNED,
        REQUEST_ACCEPTED,
        REQUEST_REJECTED
    }

    public enum EventStatus {
        PENDING,
        SENT,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventType eventType;

    @Column(nullable = false, length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * BCP-47 language tag (e.g. {@code pt-BR}, {@code en-US}) captured at publish
     * time so the asynchronous sender renders the email shell in the same locale
     * as the pre-resolved subject/body. Nullable for backward compatibility;
     * sender falls back to the default locale when absent.
     */
    @Column(length = 10)
    private String locale;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EventStatus status = EventStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private int retryCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    private OffsetDateTime processedAt;
    private OffsetDateTime nextRetryAt;
}
