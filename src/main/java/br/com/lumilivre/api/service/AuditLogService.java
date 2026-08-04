package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.model.AuditLog;
import br.com.lumilivre.api.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/** Consulta paginada da trilha de auditoria ({@link AuditLog}) para o viewer ADMIN. */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String action, String result, String actor,
            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(action)) predicates.add(cb.equal(root.get("action"), action));
            if (hasText(result)) predicates.add(cb.equal(root.get("result"), result));
            if (hasText(actor)) {
                predicates.add(cb.like(cb.lower(root.get("actor")),
                        "%" + actor.toLowerCase(Locale.ROOT) + "%"));
            }
            if (from != null)    predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null)      predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return auditLogRepository.findAll(spec, AccessLogService.newestFirst(pageable));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
