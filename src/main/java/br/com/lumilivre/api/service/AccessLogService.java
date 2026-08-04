package br.com.lumilivre.api.service;

import java.time.OffsetDateTime;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.enums.AccessEvent;
import br.com.lumilivre.api.model.AccessLog;
import br.com.lumilivre.api.repository.AccessLogRepository;
import br.com.lumilivre.api.security.ClientIpResolver;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Escrita da trilha de acessos ({@link AccessLog}) + métrica de logins por canal.
 * Nunca propaga exceção: falha de auditoria não pode derrubar a requisição.
 */
@Service
@RequiredArgsConstructor
public class AccessLogService {

    private static final Logger log = LoggerFactory.getLogger(AccessLogService.class);

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";
    public static final String RESULT_DENIED = "DENIED";

    private final AccessLogRepository accessLogRepository;
    private final ClientIpResolver clientIpResolver;
    private final MeterRegistry meterRegistry;

    public void record(AccessEvent event, String actor, String actorRole, String result, String errorMessage) {
        try {
            HttpServletRequest request = clientIpResolver.currentRequest();
            String channel = resolveChannel(request);

            accessLogRepository.save(AccessLog.builder()
                    .actor(actor != null ? actor : "anonymous")
                    .actorRole(actorRole)
                    .event(event.name())
                    .channel(channel)
                    .result(result)
                    .ipAddress(clientIpResolver.resolve(request))
                    .userAgent(header(request, "User-Agent"))
                    .correlationId(MDC.get("correlationId"))
                    .errorMessage(errorMessage)
                    .occurredAt(OffsetDateTime.now())
                    .build());

            meterRegistry.counter("access.events",
                    "event", event.name(),
                    "channel", channel,
                    "result", result).increment();
        } catch (Exception e) {
            log.warn("AccessLogService: failed to persist access event {} for {}: {}",
                    event, actor, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AccessLog> search(String event, String channel, String result, String actor, String ip,
            OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        return accessLogRepository.search(event, channel, result, actor, ip, from, to, pageable);
    }

    /** Canal derivado do header explícito X-Client; fallback pelo User-Agent. */
    private String resolveChannel(HttpServletRequest request) {
        String client = header(request, "X-Client");
        if (client != null && !client.isBlank()) {
            String upper = client.trim().toUpperCase(Locale.ROOT);
            if (upper.equals("WEB") || upper.equals("APP")) {
                return upper;
            }
        }
        String ua = header(request, "User-Agent");
        if (ua == null || ua.isBlank()) {
            return "UNKNOWN";
        }
        String uaLower = ua.toLowerCase(Locale.ROOT);
        if (uaLower.contains("dart") || uaLower.contains("dio") || uaLower.contains("flutter") || uaLower.contains("okhttp")) {
            return "APP";
        }
        if (uaLower.contains("mozilla") || uaLower.contains("chrome") || uaLower.contains("safari") || uaLower.contains("firefox")) {
            return "WEB";
        }
        return "UNKNOWN";
    }

    private String header(HttpServletRequest request, String name) {
        return request != null ? request.getHeader(name) : null;
    }
}
