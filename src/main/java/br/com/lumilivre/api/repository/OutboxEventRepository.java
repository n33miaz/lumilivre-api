package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.lumilivre.api.model.OutboxEvent;
import br.com.lumilivre.api.model.OutboxEvent.EventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(EventStatus status);

    List<OutboxEvent> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetries);

    long countByStatus(EventStatus status);

    /**
     * Idade do evento pendente mais antigo. Só a contagem não distingue "cinco
     * eventos criados agora" de "cinco eventos parados há três dias" — e é a
     * segunda situação que significa que o outbox deixou de drenar.
     */
    @Query("SELECT MIN(e.createdAt) FROM OutboxEvent e WHERE e.status = :status")
    OffsetDateTime findOldestCreatedAtByStatus(EventStatus status);
}
