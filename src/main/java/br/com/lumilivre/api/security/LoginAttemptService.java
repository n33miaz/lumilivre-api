package br.com.lumilivre.api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * SEC-05: trava por CONTA (complementa o rate limit por IP do
 * {@link AuthRateLimitFilter}). Depois de {@value #MAX_ATTEMPTS} falhas de login
 * na janela, a conta fica bloqueada por {@link #LOCK}, derrotando brute force
 * distribuído (muitos IPs contra um mesmo usuário) que o limite por IP não pega.
 *
 * <p>Estado em memória (a app roda tipicamente 1 instância no Render free). Para
 * multi-instância, trocar por Redis mantendo a mesma interface. Mapa com teto de
 * tamanho para não crescer sem limite (chave é o username, não spoofável).
 */
@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);
    private static final int MAX_ENTRIES = 50_000;

    private record Attempt(int count, Instant windowStart, Instant lockedUntil) {}

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** Segundos restantes de bloqueio para a conta, ou 0 se não bloqueada. */
    public long blockedSecondsRemaining(String username) {
        Attempt a = attempts.get(key(username));
        if (a != null && a.lockedUntil() != null) {
            Instant now = Instant.now();
            if (now.isBefore(a.lockedUntil())) {
                return Math.max(1, Duration.between(now, a.lockedUntil()).toSeconds());
            }
        }
        return 0;
    }

    public boolean isBlocked(String username) {
        return blockedSecondsRemaining(username) > 0;
    }

    public void recordFailure(String username) {
        if (attempts.size() > MAX_ENTRIES) {
            attempts.clear();
        }
        Instant now = Instant.now();
        attempts.compute(key(username), (k, cur) -> {
            boolean expired = cur == null
                    || now.isAfter(cur.windowStart().plus(WINDOW))
                    || (cur.lockedUntil() != null && now.isAfter(cur.lockedUntil()));
            if (expired) {
                return new Attempt(1, now, null);
            }
            int count = cur.count() + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS ? now.plus(LOCK) : cur.lockedUntil();
            return new Attempt(count, cur.windowStart(), lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
