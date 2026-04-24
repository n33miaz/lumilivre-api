package br.com.lumilivre.api.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorOrderByOccurredAtDesc(String actor);

    List<AuditLog> findByActionAndOccurredAtBetween(String action, LocalDateTime from, LocalDateTime to);
}
