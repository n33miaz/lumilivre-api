package br.com.lumilivre.api.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AuditLog;

// Filtros dinâmicos via Specification (montados no AuditLogService): o padrão
// JPQL "(:p IS NULL OR ...)" quebra no PostgreSQL, que não infere o tipo dos
// parâmetros nulos ("could not determine data type of parameter").
@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByActorOrderByOccurredAtDesc(String actor);

    List<AuditLog> findByActionAndOccurredAtBetween(String action, OffsetDateTime from, OffsetDateTime to);
}
