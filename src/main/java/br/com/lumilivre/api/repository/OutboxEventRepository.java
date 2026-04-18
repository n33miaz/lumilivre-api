package br.com.lumilivre.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.OutboxEventModel;
import br.com.lumilivre.api.model.OutboxEventModel.EventStatus;

public interface OutboxEventRepository extends JpaRepository<OutboxEventModel, Long> {

    List<OutboxEventModel> findTop50ByStatusOrderByCreatedAtAsc(EventStatus status);

    List<OutboxEventModel> findByStatusAndRetryCountLessThan(EventStatus status, int maxRetries);
}
