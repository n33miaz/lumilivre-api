package br.com.lumilivre.api.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/**
 * Trava por CONTA (complementa o rate limit por IP do
 * {@link AuthRateLimitFilter}). Depois de um número configurado de falhas de
 * login na janela, a conta fica bloqueada por um tempo também configurado,
 * derrotando brute force distribuído (muitos IPs contra um mesmo usuário) que o
 * limite por IP não pega.
 *
 * <p>Os números vêm de {@link RateLimitProperties.AccountLock} — cinco falhas em
 * quinze minutos, bloqueio de quinze, quando ninguém configura nada.
 *
 * <p>Estado em memória (a app roda tipicamente 1 instância no Render free). Para
 * multi-instância, trocar por Redis mantendo a mesma interface. Mapa com teto de
 * tamanho para não crescer sem limite (chave é o username, não spoofável).
 */
@Service
@EnableConfigurationProperties(RateLimitProperties.class)
public class LoginAttemptService {

    private static final int MAX_ENTRIES = 50_000;

    private record Attempt(int count, Instant windowStart, Instant lockedUntil) {}

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private final int maxAttempts;
    private final Duration window;
    private final Duration lock;
    private final Clock clock;

    // Com dois construtores o Spring nao tem como escolher sozinho; este e o de
    // producao, o outro existe so para o teste controlar o relogio.
    @Autowired
    public LoginAttemptService(RateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * O relógio é injetável só para o teste conseguir provar que insistir não
     * empurra o desbloqueio: medindo em segundos truncados, a diferença de uma
     * chamada para a outra some no arredondamento e o teste passaria de qualquer
     * jeito.
     */
    LoginAttemptService(RateLimitProperties properties, Clock clock) {
        RateLimitProperties.AccountLock config = properties.getAccountLock();
        this.maxAttempts = config.getMaxAttempts();
        this.window = config.getWindow();
        this.lock = config.getLockDuration();
        this.clock = clock;
    }

    /** Falhas toleradas antes do bloqueio — o teste lê daqui em vez de cravar 5. */
    int maxAttempts() {
        return maxAttempts;
    }

    /** Segundos restantes de bloqueio para a conta, ou 0 se não bloqueada. */
    public long blockedSecondsRemaining(String username) {
        Attempt a = attempts.get(key(username));
        if (a != null && a.lockedUntil() != null) {
            Instant now = Instant.now(clock);
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
        Instant now = Instant.now(clock);
        attempts.compute(key(username), (k, cur) -> {
            boolean expired = cur == null
                    || now.isAfter(cur.windowStart().plus(window))
                    || (cur.lockedUntil() != null && now.isAfter(cur.lockedUntil()));

            // A janela nova comeca do zero, mas o teto vale ja na primeira falha:
            // com maxAttempts=1 o ramo de reinicio devolvia contagem 1 sem nunca
            // olhar o limite, e a conta jamais travava. Com 5 cravado o defeito
            // era inalcancavel; configuravel, deixa de ser.
            int count = expired ? 1 : cur.count() + 1;
            Instant windowStart = expired ? now : cur.windowStart();

            // Ja travada e ainda dentro do prazo: mantem o instante original. Sem
            // isso cada nova tentativa reagenda o desbloqueio para agora+lock, e
            // quem insiste mantem a conta alheia trancada para sempre — a trava
            // contra brute force viraria negacao de servico contra o dono dela.
            boolean aindaTravada = !expired && cur.lockedUntil() != null;
            Instant lockedUntil;
            if (aindaTravada) {
                lockedUntil = cur.lockedUntil();
            } else if (count >= maxAttempts) {
                lockedUntil = now.plus(lock);
            } else {
                lockedUntil = null;
            }
            return new Attempt(count, windowStart, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
