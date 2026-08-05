package br.com.lumilivre.api.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.common.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Rate limit para endpoints de autenticação: 5 requisições por 10 minutos por IP.
 * Retorna HTTP 429 com Retry-After na 6ª tentativa dentro da janela.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    // Teto de baldes para evitar crescimento ilimitado do mapa (DoS de memória).
    private static final int MAX_BUCKETS = 50_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Casa com os paths REAIS (@RequestMapping("/api/auth")). Antes
        // comparava "/auth/login", que nunca batia, então o filtro nunca throttava.
        // getServletPath() é decodificado/normalizado — getRequestURI() permitiria
        // burlar o limite com percent-encoding (ex.: /api/%61uth/login).
        String path = request.getServletPath();
        return !("/api/auth/login".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path)
                // validate-token ficou de fora na primeira passada: sem limite,
                // dá para varrer UUID de reset em paralelo. O token vai no path,
                // então a comparação é por prefixo.
                || path.startsWith("/api/auth/validate-token/"));
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
        Bucket bucket = buckets.computeIfAbsent(bucketKey(ip, request), k -> buildBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        writeTooManyRequests(request, response);
    }

    /**
     * Mesmo envelope {@link ErrorResponse} do resto da API. Antes este 429 saía
     * com chaves em português ({@code erro}/{@code mensagem}) e mensagem fixa em
     * pt-BR, então um cliente que trata erro de forma genérica não entendia esta
     * resposta — justamente a de um endpoint de autenticação.
     */
    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Locale locale = resolveLocale(request);
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(messageResolver.resolve("error.rate-limit.title", locale))
                .message(messageResolver.resolve("error.rate-limit.message", locale, WINDOW.toMinutes()))
                .path(request.getRequestURI())
                .correlationId(MDC.get("correlationId"))
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Language", locale.toLanguageTag());
        response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
        objectMapper.writeValue(response.getWriter(), body);
    }

    /** O filtro roda antes do DispatcherServlet, então o locale vem do header. */
    private Locale resolveLocale(HttpServletRequest request) {
        Locale requested = request.getLocale();
        if (requested != null && "en".equals(requested.getLanguage())) {
            return Locale.forLanguageTag("en-US");
        }
        return Locale.forLanguageTag("pt-BR");
    }

    /**
     * Balde próprio para {@code validate-token}: se dividisse o balde com
     * login/forgot/reset, o fluxo normal de recuperação (pedir + validar +
     * trocar, com um recarregamento de página no meio) consumiria a cota de
     * login do usuário. Cada grupo continua com o mesmo teto por IP.
     */
    private String bucketKey(String ip, HttpServletRequest request) {
        String group = request.getServletPath().startsWith("/api/auth/validate-token/")
                ? "validate-token"
                : "auth";
        return ip + "|" + group;
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
