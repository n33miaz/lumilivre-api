package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AuditLogModel;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogModel, Long> {

    List<AuditLogModel> findByActorOrderByOccurredAtDesc(String actor);

    List<AuditLogModel> findByActionAndOccurredAtBetween(String action, LocalDateTime from, LocalDateTime to);
}
