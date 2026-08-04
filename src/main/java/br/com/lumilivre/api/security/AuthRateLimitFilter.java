package br.com.lumilivre.api.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate limit para endpoints de autenticação: 5 requisições por 10 minutos por IP.
 * Retorna HTTP 429 com Retry-After na 6ª tentativa dentro da janela.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    // Teto de baldes para evitar crescimento ilimitado do mapa (DoS de memória).
    private static final int MAX_BUCKETS = 50_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Casa com os paths REAIS (@RequestMapping("/api/auth")). Antes
        // comparava "/auth/login", que nunca batia, então o filtro nunca throttava.
        // getServletPath() é decodificado/normalizado — getRequestURI() permitiria
        // burlar o limite com percent-encoding (ex.: /api/%61uth/login).
        String path = request.getServletPath();
        return !("/api/auth/login".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = resolveClientIp(request);
        // Eviction simples: chave é o IP resolvido pelo proxy (não spoofável), mas
        // ainda assim limitamos o tamanho do mapa por segurança.
        if (buckets.size() > MAX_BUCKETS) {
            buckets.clear();
        }
        Bucket bucket = buckets.computeIfAbsent(ip, k -> buildBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            response.getWriter().write(
                    "{\"status\":429,\"erro\":\"Too Many Requests\"," +
                    "\"mensagem\":\"Limite de tentativas excedido. Tente novamente em " +
                    WINDOW.toMinutes() + " minuto(s).\"}");
        }
    }

    private Bucket buildBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS)
                .refillIntervally(MAX_REQUESTS, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Com server.forward-headers-strategy=framework, getRemoteAddr() já é o IP
        // real resolvido pelo proxy confiável. NÃO parseamos X-Forwarded-For do
        // cliente (falsificável → burla o limite + DoS de memória).
        String ip = request.getRemoteAddr();
        return (ip != null && !ip.isBlank()) ? ip : "unknown";
    }
}
