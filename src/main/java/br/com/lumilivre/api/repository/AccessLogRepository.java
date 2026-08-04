package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AccessLog;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    @Query("""
            SELECT a FROM AccessLog a
            WHERE (:event   IS NULL OR a.event   = :event)
              AND (:channel IS NULL OR a.channel = :channel)
              AND (:result  IS NULL OR a.result  = :result)
              AND (:actor   IS NULL OR LOWER(a.actor) LIKE LOWER(CONCAT('%', :actor, '%')))
              AND (:ip      IS NULL OR a.ipAddress LIKE CONCAT('%', :ip, '%'))
              AND (:from    IS NULL OR a.occurredAt >= :from)
              AND (:to      IS NULL OR a.occurredAt <= :to)
            ORDER BY a.occurredAt DESC
            """)
    Page<AccessLog> search(@Param("event") String event,
                           @Param("channel") String channel,
                           @Param("result") String result,
                           @Param("actor") String actor,
                           @Param("ip") String ip,
                           @Param("from") OffsetDateTime from,
                           @Param("to") OffsetDateTime to,
                           Pageable pageable);
}
