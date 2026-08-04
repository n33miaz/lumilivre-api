package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.model.AuditLog;
import br.com.lumilivre.api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

/** Consulta paginada da trilha de auditoria ({@link AuditLog}) para o viewer ADMIN. */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String action, String result, String actor,
            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return auditLogRepository.search(action, result, actor, from, to, pageable);
    }
}
