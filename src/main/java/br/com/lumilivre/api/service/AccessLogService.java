package br.com.lumilivre.api.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import br.com.lumilivre.api.enums.AccessEvent;
import br.com.lumilivre.api.model.AccessLog;
import br.com.lumilivre.api.repository.AccessLogRepository;
import br.com.lumilivre.api.security.ClientIpResolver;
import br.com.lumilivre.api.security.CustomUserDetails;
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

    /**
     * Janela de deduplicação dos eventos de uso. 30 min é o intervalo em que
     * abrir a mesma tela de novo continua sendo "a mesma visita à biblioteca";
     * curto o bastante para separar a consulta da manhã da consulta da tarde.
     */
    private static final Duration USAGE_WINDOW = Duration.ofMinutes(30);

    private static final int MAX_USAGE_KEYS = 20_000;

    private final Map<String, OffsetDateTime> recentUsage = new ConcurrentHashMap<>();

    private final AccessLogRepository accessLogRepository;
    private final ClientIpResolver clientIpResolver;
    private final MeterRegistry meterRegistry;

    public void record(AccessEvent event, String actor, String actorRole, String result, String errorMessage) {
        persist(event, actor, actorRole, result, null, errorMessage);
    }

    /**
     * Recusa de acesso a um recurso identificado (tentativa de IDOR).
     *
     * <p>Sem deduplicação, ao contrário dos eventos de uso: aqui a repetição é o
     * sinal — uma matrícula varrendo dezenas de outras em sequência é exatamente
     * o que se quer ver na trilha.
     */
    public void recordDenied(String actor, String actorRole, String targetId, String reason) {
        persist(AccessEvent.ACCESS_DENIED, actor, actorRole, RESULT_DENIED, targetId, reason);
    }

    /**
     * Registra <b>uso</b> da biblioteca pelo usuário autenticado corrente.
     *
     * <p>Três regras de contenção, que são o que separa auditoria de log de
     * servidor (ver {@link br.com.lumilivre.api.security.AccessAudited}):
     * <ol>
     *   <li>Sem principal identificado, não grava. Uso anônimo (convidado,
     *       robô) vira contador Prometheus — se gravasse linha, qualquer
     *       varredura no catálogo público escreveria no banco por nós.</li>
     *   <li>Uma linha por (pessoa, evento, alvo) em {@link #USAGE_WINDOW}. A
     *       tela do app dispara várias chamadas ao abrir; a trilha registra a
     *       intenção, não a requisição.</li>
     *   <li>Nada disso derruba a requisição: falha de auditoria é log de
     *       warning, como no restante da classe.</li>
     * </ol>
     */
    public void recordUsage(AccessEvent event, String targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = actorOf(auth);
        if (actor == null) {
            // Convidado: só o contador, sem linha. O agregado responde "quanto
            // o catálogo público é usado" sem transformar a tabela em access log
            // de webserver — e sem dar ao anônimo uma escrita no banco.
            meterRegistry.counter("access.usage.anonymous", "event", event.name()).increment();
            return;
        }
        if (!shouldRecordUsage(actor, event, targetId)) {
            return;
        }
        persist(event, actor, roleOf(auth), RESULT_SUCCESS, targetId, null);
    }

    private void persist(AccessEvent event, String actor, String actorRole, String result,
            String targetId, String errorMessage) {
        try {
            HttpServletRequest request = clientIpResolver.currentRequest();
            String channel = resolveChannel(request);

            accessLogRepository.save(AccessLog.builder()
                    .actor(actor != null ? actor : "anonymous")
                    .actorRole(actorRole)
                    .event(event.name())
                    .channel(channel)
                    .result(result)
                    .targetId(targetId)
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

    /**
     * Deduplicação em memória, sem SELECT: os eventos de uso ficam no caminho de
     * leituras quentes (catálogo, ficha de livro) e um round-trip extra por
     * requisição para descobrir "já gravei isto?" custaria mais que a própria
     * consulta auditada.
     *
     * <p>A garantia que a auditoria precisa é "se a pessoa usou o acervo na
     * janela, existe <i>ao menos</i> uma linha" — e essa se mantém. O que se
     * perde é "exatamente uma": se o processo reiniciar ou houver duas
     * instâncias, sai uma linha por instância por janela. Aceitável; o inverso
     * (perder o registro) não seria.
     */
    private boolean shouldRecordUsage(String actor, AccessEvent event, String targetId) {
        OffsetDateTime now = OffsetDateTime.now();
        // Teto de chaves para não virar vetor de consumo de memória; ao estourar,
        // limpa tudo (mesma estratégia do AuthRateLimitFilter) — o efeito é
        // apenas uma linha extra por pessoa na janela seguinte.
        if (recentUsage.size() > MAX_USAGE_KEYS) {
            recentUsage.clear();
        }
        String key = actor + '|' + event.name() + '|' + (targetId != null ? targetId : "-");
        OffsetDateTime previous = recentUsage.get(key);
        if (previous != null && previous.isAfter(now.minus(USAGE_WINDOW))) {
            return false;
        }
        recentUsage.put(key, now);
        return true;
    }

    /**
     * Identificador do ator na trilha: <b>matrícula</b> para leitor, login para
     * o resto.
     *
     * <p>Público e estático porque a trilha só serve se o mesmo ator tiver sempre
     * o mesmo nome: com o login gravando o e-mail e o evento de uso gravando a
     * matrícula, não havia como ligar "entrou" a "consultou o acervo" da mesma
     * pessoa — e é essa a pergunta.
     *
     * @return o identificador, ou {@code null} quando não há ninguém autenticado
     */
    public static String actorOf(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (auth.getPrincipal() instanceof CustomUserDetails details) {
            var reader = details.getAppUser().getReader();
            return reader != null ? reader.getRegistrationNumber() : details.getUsername();
        }
        return auth.getName();
    }

    /** Papel com o prefixo {@code ROLE_}, como gravado em {@code actor_role}. */
    public static String roleOf(Authentication auth) {
        if (auth == null) {
            return "UNKNOWN";
        }
        return auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("UNKNOWN");
    }

    @Transactional(readOnly = true)
    public Page<AccessLog> search(String event, String channel, String result, String actor, String ip,
            String target, OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Specification<AccessLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(event))   predicates.add(cb.equal(root.get("event"), event));
            if (hasText(channel)) predicates.add(cb.equal(root.get("channel"), channel));
            if (hasText(result))  predicates.add(cb.equal(root.get("result"), result));
            if (hasText(actor)) {
                predicates.add(cb.like(cb.lower(root.get("actor")),
                        "%" + actor.toLowerCase(Locale.ROOT) + "%"));
            }
            if (hasText(ip))      predicates.add(cb.like(root.get("ipAddress"), "%" + ip + "%"));
            if (hasText(target))  predicates.add(cb.equal(root.get("targetId"), target));
            if (from != null)     predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null)       predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return accessLogRepository.findAll(spec, newestFirst(pageable));
    }

    /** Sem sort explícito do cliente, a trilha é exibida do mais recente para o mais antigo. */
    static Pageable newestFirst(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "occurredAt"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
