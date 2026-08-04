package br.com.lumilivre.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AccessLog;

// Filtros dinâmicos via Specification (montados no AccessLogService): o padrão
// JPQL "(:p IS NULL OR ...)" quebra no PostgreSQL, que não infere o tipo dos
// parâmetros nulos ("could not determine data type of parameter").
@Repository
public interface AccessLogRepository
        extends JpaRepository<AccessLog, Long>, JpaSpecificationExecutor<AccessLog> {
}
