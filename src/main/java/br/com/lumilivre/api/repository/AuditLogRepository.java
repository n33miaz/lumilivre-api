package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorOrderByOccurredAtDesc(String actor);

    List<AuditLog> findByActionAndOccurredAtBetween(String action, OffsetDateTime from, OffsetDateTime to);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:action IS NULL OR a.action = :action)
              AND (:result IS NULL OR a.result = :result)
              AND (:actor  IS NULL OR LOWER(a.actor) LIKE LOWER(CONCAT('%', :actor, '%')))
              AND (:from   IS NULL OR a.occurredAt >= :from)
              AND (:to     IS NULL OR a.occurredAt <= :to)
            ORDER BY a.occurredAt DESC
            """)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("result") String result,
                          @Param("actor") String actor,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to,
                          Pageable pageable);
}
