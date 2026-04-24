package br.com.lumilivre.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.OutboxEvent;
import br.com.lumilivre.api.model.OutboxEvent.EventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(EventStatus status);

    List<OutboxEvent> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetries);
}
