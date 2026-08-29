package br.com.lumilivre.api.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

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
 * Rate limit por IP, com dois grupos de balde e limites diferentes.
 *
 * <p>Os tetos vêm de {@link RateLimitProperties} — os defaults descritos abaixo
 * são os mesmos números que antes viviam cravados aqui.
 *
 * <p><b>Autenticação</b> ({@code /api/auth/login}, {@code forgot-password},
 * {@code reset-password}, {@code validate-token/**}): 5 requisições por 10
 * minutos. É limite de adivinhação de credencial — apertado de propósito.
 *
 * <p><b>Leituras do acervo</b> ({@code /api/books/**}, {@code /api/settings/public},
 * {@code /api/app-version}): 300 por minuto. Existe porque
 * boa parte dessas rotas é anônima — a ficha do livro passou a ser, e catálogo,
 * busca pública e navegação por gênero já eram — e endpoint anônimo é superfície
 * de DoS. O teto é folgado de propósito: uma tela do app dispara meia dúzia de
 * chamadas e um balcão inteiro atrás do mesmo IP não deve encostar nele; o que
 * ele corta é varredura automatizada. Volume de tráfego de verdade se barra na
 * borda (proxy/CDN), não em processo — aqui já se pagou servlet e JSON.
 *
 * <p><b>Marcar interesse</b> ({@code POST}/{@code DELETE}
 * {@code /api/books/{id}/interest}): 120 por minuto.
 * Tem balde próprio, e não o de leitura, por dois motivos. É escrita autenticada
 * e não leitura anônima — o custo, o abuso possível e o teto natural são outros
 * (a unicidade da V8 limita cada leitor ao tamanho do acervo, ele não consegue
 * criar duas linhas para o mesmo livro por mais que insista). E, principalmente,
 * porque compartilhar balde faria um laço de "marcar interesse" consumir a cota
 * de <i>leitura do catálogo</i> do mesmo IP: numa escola atrás de um NAT único,
 * um aluno com script deixaria a turma inteira sem catálogo. Balde separado
 * transforma isso em "ele mesmo para de marcar interesse".
 *
 * <p>Retorna HTTP 429 com {@code Retry-After} ao estourar o balde do grupo.
 */
@Component
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    // O default de interesse (120/min) é folgado de propósito: uma pessoa tocando
    // em coração faz umas dezenas por minuto no limite, e o NAT da escola soma
    // várias. Folgado para uso humano, apertado para laço automatizado.

    private static final String GROUP_AUTH = "auth";
    private static final String GROUP_VALIDATE_TOKEN = "validate-token";
    private static final String GROUP_PUBLIC_READ = "public-read";
    private static final String GROUP_INTEREST_WRITE = "interest-write";

    // Teto de baldes para evitar crescimento ilimitado do mapa (DoS de memória).
    private static final int MAX_BUCKETS = 50_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;
    private final LocaleResolver localeResolver;
    private final RateLimitProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return resolveGroup(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String group = resolveGroup(request);
        String ip = resolveClientIp(request);
        // Eviction simples: chave é o IP resolvido pelo proxy (não spoofável), mas
        // ainda assim limitamos o tamanho do mapa por segurança.
        if (buckets.size() > MAX_BUCKETS) {
            buckets.clear();
        }
        Bucket bucket = buckets.computeIfAbsent(ip + "|" + group, k -> buildBucket(group));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        writeTooManyRequests(request, response, group);
    }

    /**
     * Grupo de balde da requisição, ou {@code null} quando ela não é limitada.
     *
     * <p>Casa com os paths REAIS (@RequestMapping("/api/auth")). Antes comparava
     * "/auth/login", que nunca batia, então o filtro nunca throttava.
     * {@code getServletPath()} é decodificado/normalizado — {@code getRequestURI()}
     * permitiria burlar o limite com percent-encoding (ex.: /api/%61uth/login).
     *
     * <p>{@code validate-token} tem balde próprio: se dividisse com
     * login/forgot/reset, o fluxo normal de recuperação (pedir + validar + trocar,
     * com um recarregamento de página no meio) consumiria a cota de login do
     * usuário.
     */
    private String resolveGroup(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null) {
            return null;
        }
        if ("/api/auth/login".equals(path)
                || "/api/auth/forgot-password".equals(path)
                || "/api/auth/reset-password".equals(path)) {
            return GROUP_AUTH;
        }
        // O token vai no path, então a comparação é por prefixo.
        if (path.startsWith("/api/auth/validate-token/")) {
            return GROUP_VALIDATE_TOKEN;
        }
        // Antes do prefixo /api/books, senão a escrita cairia no balde de
        // leitura do catálogo (que é o que se quer evitar).
        if (path.startsWith("/api/books/") && path.endsWith("/interest")) {
            return GROUP_INTEREST_WRITE;
        }
        if (path.startsWith("/api/books")
                || "/api/settings/public".equals(path)
                || "/api/app-version".equals(path)) {
            return GROUP_PUBLIC_READ;
        }
        return null;
    }

    /**
     * Mesmo envelope {@link ErrorResponse} do resto da API. Antes este 429 saía
     * com chaves em português ({@code erro}/{@code mensagem}) e mensagem fixa em
     * pt-BR, então um cliente que trata erro de forma genérica não entendia esta
     * resposta — justamente a de um endpoint de autenticação.
     */
    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response, String group)
            throws IOException {
        Locale locale = resolveLocale(request);
        Duration window = windowOf(group);
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(messageResolver.resolve("error.rate-limit.title", locale))
                .message(messageResolver.resolve("error.rate-limit.message", locale, window.toMinutes()))
                .path(request.getRequestURI())
                .correlationId(MDC.get("correlationId"))
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Language", locale.toLanguageTag());
        response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * O filtro roda antes do DispatcherServlet, então resolve o locale por conta
     * própria — mas pelo mesmo {@link LocaleResolver} que o dispatcher usa depois.
     * Um {@code if} de dois idiomas aqui deixaria es/zh/hi recebendo este 429 em
     * português, e a lista de suportados viveria em dois lugares.
     */
    private Locale resolveLocale(HttpServletRequest request) {
        return localeResolver.resolveLocale(request);
    }

    /**
     * O balde configurado do grupo. {@code validate-token} tem entrada própria
     * mesmo com os mesmos defaults do auth: eles nunca dividiram cota (a chave do
     * mapa inclui o grupo), e separar aqui deixa a instância de teste afrouxar um
     * sem afrouxar o outro.
     */
    private RateLimitProperties.Bucket configOf(String group) {
        return switch (group) {
            case GROUP_PUBLIC_READ -> properties.getPublicRead();
            case GROUP_INTEREST_WRITE -> properties.getInterestWrite();
            case GROUP_VALIDATE_TOKEN -> properties.getValidateToken();
            default -> properties.getAuth();
        };
    }

    private Duration windowOf(String group) {
        return configOf(group).getWindow();
    }

    private Bucket buildBucket(String group) {
        int capacity = configOf(group).getCapacity();
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(capacity, windowOf(group))
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
