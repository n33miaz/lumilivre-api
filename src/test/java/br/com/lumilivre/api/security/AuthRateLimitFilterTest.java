package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Locale;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * O filtro deve throttlar os endpoints REAIS de auth
 * (/api/auth/**) por IP resolvido pelo proxy — e NÃO deve confiar em
 * X-Forwarded-For do cliente (falsificável).
 */
class AuthRateLimitFilterTest {

    @Test
    void authEndpointsAllowFiveRequestsAndRejectTheSixthForSameIp() throws Exception {
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest("203.0.113.10"), response, chain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.10"), blocked, chain);

        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("600");
        // Mesmo envelope do resto da API: chaves em inglês, não erro/mensagem.
        assertThat(blocked.getContentAsString())
                .contains("\"status\":429")
                .contains("\"error\":")
                .contains("\"message\":")
                .contains("\"path\":\"/api/auth/login\"")
                .doesNotContain("\"erro\"")
                .doesNotContain("\"mensagem\"");
    }

    @Test
    void tooManyRequestsBodyIsLocalized() throws Exception {
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(loginRequest("203.0.113.80"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest ptRequest = loginRequest("203.0.113.80");
        ptRequest.addPreferredLocale(Locale.forLanguageTag("pt-BR"));
        MockHttpServletResponse ptResponse = new MockHttpServletResponse();
        ptResponse.setCharacterEncoding("UTF-8");
        filter.doFilter(ptRequest, ptResponse, chain);

        assertThat(ptResponse.getHeader("Content-Language")).isEqualTo("pt-BR");
        assertThat(ptResponse.getContentAsString()).contains("Muitas requisições");

        MockHttpServletRequest enRequest = loginRequest("203.0.113.80");
        enRequest.addPreferredLocale(Locale.forLanguageTag("en-US"));
        MockHttpServletResponse enResponse = new MockHttpServletResponse();
        enResponse.setCharacterEncoding("UTF-8");
        filter.doFilter(enRequest, enResponse, chain);

        assertThat(enResponse.getHeader("Content-Language")).isEqualTo("en-US");
        assertThat(enResponse.getContentAsString()).contains("Too many requests");
    }

    @Test
    void forwardedForHeaderIsIgnoredForRateLimiting() throws Exception {
        // Mesmo IP resolvido pelo proxy (remoteAddr) com X-Forwarded-For variável
        // NÃO deve burlar o limite: o filtro ignora o header do cliente.
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(forwardedLoginRequest("10.0.0.99", "198.51.100." + i), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(forwardedLoginRequest("10.0.0.99", "198.51.100.250"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void nonAuthEndpointsBypassRateLimit() throws Exception {
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        request.setRemoteAddr("203.0.113.20");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void forgotPasswordEndpointUsesSameLimit() throws Exception {
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(authRequest("/api/auth/forgot-password", "203.0.113.30"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequest("/api/auth/forgot-password", "203.0.113.30"), blocked, chain);

        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void validateResetTokenEndpointIsThrottledDespiteTheTokenInThePath() throws Exception {
        // Sem limite aqui, um atacante varre UUID de reset em paralelo.
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(authRequest("/api/auth/validate-token/token-" + i, "203.0.113.60"),
                    new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequest("/api/auth/validate-token/token-x", "203.0.113.60"), blocked, chain);

        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void validateResetTokenDoesNotEatTheLoginBudgetOfTheSameIp() throws Exception {
        // O fluxo de recuperação (pedir + validar + trocar, com um recarregamento
        // no meio) não pode deixar o usuário sem tentativas de login.
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 6; i++) {
            filter.doFilter(authRequest("/api/auth/validate-token/token-" + i, "203.0.113.70"),
                    new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse login = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.70"), login, chain);

        assertThat(login.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void blockedRequestDoesNotContinueFilterChain() throws Exception {
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 6; i++) {
            filter.doFilter(loginRequest("203.0.113.40"), new MockHttpServletResponse(), chain);
        }

        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(chain, never()).doFilter(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void interestWritesHaveTheirOwnCeiling() throws Exception {
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 120; i++) {
            MockHttpServletResponse allowed = new MockHttpServletResponse();
            filter.doFilter(interestRequest("POST", "198.51.100.10"), allowed, chain);
            assertThat(allowed.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(interestRequest("POST", "198.51.100.10"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        // Janela de um minuto, não os 10 minutos do balde de autenticação.
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void aLoopOfInterestWritesDoesNotLeaveTheSameIpWithoutCatalogue() throws Exception {
        // O motivo de o interesse ter balde próprio: numa escola atrás de um NAT
        // único, um aluno com script não pode deixar a turma sem catálogo.
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 200; i++) {
            filter.doFilter(interestRequest("POST", "198.51.100.20"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse catalogue = new MockHttpServletResponse();
        filter.doFilter(publicReadRequest("/api/books/catalog", "198.51.100.20"), catalogue, chain);

        assertThat(catalogue.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void removingInterestSharesTheBucketWithMarkingIt() throws Exception {
        // Marcar e desmarcar em laço é o mesmo abuso, então é a mesma cota.
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 120; i++) {
            filter.doFilter(interestRequest(i % 2 == 0 ? "POST" : "DELETE", "198.51.100.30"),
                    new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(interestRequest("DELETE", "198.51.100.30"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    private static MockHttpServletRequest interestRequest(String method, String remoteAddr) {
        String uri = "/api/books/00000000-0000-4000-8000-000000003086/interest";
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServletPath(uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static MockHttpServletRequest publicReadRequest(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setServletPath(uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private static AuthRateLimitFilter newFilter() {
        return newFilter(new RateLimitProperties());
    }

    private static AuthRateLimitFilter newFilter(RateLimitProperties properties) {
        // ObjectMapper pelo builder do Spring (registra o módulo de java.time,
        // igual ao bean injetado em produção), MessageResolver com os bundles de
        // verdade e o LocaleResolver real — assim o corpo do 429 é exercitado,
        // não simulado, e no idioma que o header pediu.
        I18nConfig i18n = new I18nConfig();
        return new AuthRateLimitFilter(
                Jackson2ObjectMapperBuilder.json().build(),
                new MessageResolver(i18n.messageSource()),
                i18n.localeResolver(),
                properties);
    }

    private static MockHttpServletRequest loginRequest(String remoteAddr) {
        return authRequest("/api/auth/login", remoteAddr);
    }

    private static MockHttpServletRequest forwardedLoginRequest(String remoteAddr, String forwardedFor) {
        MockHttpServletRequest request = loginRequest(remoteAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    @Test
    void percentEncodedPathDoesNotBypassRateLimit() throws Exception {
        // O container decodifica o path (servletPath). Como o filtro compara o
        // servletPath — e não o requestURI cru — /api/%61uth/login não burla.
        AuthRateLimitFilter filter = newFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 6; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/%61uth/login");
            request.setServletPath("/api/auth/login");
            request.setRemoteAddr("203.0.113.50");
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        }

        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    /**
     * O teto configurado precisa chegar ao balde de verdade, senão a instância de
     * teste continuaria apanhando do 429 e a configuração seria decorativa.
     */
    @Test
    void oTetoDeAuthVemDaConfiguracaoENaoDeConstante() throws Exception {
        RateLimitProperties afrouxado = new RateLimitProperties();
        afrouxado.getAuth().setCapacity(12);
        AuthRateLimitFilter filter = newFilter(afrouxado);
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 13; i++) {
            filter.doFilter(loginRequest("203.0.113.90"), new MockHttpServletResponse(), chain);
        }

        verify(chain, times(12)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    /**
     * A janela configurada também precisa sair no {@code Retry-After}: um cliente
     * que respeita o cabeçalho voltaria cedo demais se ele mentisse o default.
     */
    @Test
    void oRetryAfterRefleteAJanelaConfigurada() throws Exception {
        RateLimitProperties apertado = new RateLimitProperties();
        apertado.getAuth().setCapacity(1);
        apertado.getAuth().setWindow(java.time.Duration.ofMinutes(3));
        AuthRateLimitFilter filter = newFilter(apertado);
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        filter.doFilter(loginRequest("203.0.113.91"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("203.0.113.91"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("180");
    }

    /**
     * Afrouxar a leitura pública não pode afrouxar o login junto — é justamente
     * a separação de baldes que permite dar folga ao pentest sem abrir a porta
     * de adivinhação de credencial.
     */
    @Test
    void afrouxarLeituraPublicaNaoMexeNoBaldeDeAuth() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getPublicRead().setCapacity(10_000);
        AuthRateLimitFilter filter = newFilter(properties);
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 6; i++) {
            filter.doFilter(loginRequest("203.0.113.92"), new MockHttpServletResponse(), chain);
        }

        verify(chain, times(5)).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private static MockHttpServletRequest authRequest(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        // Espelha o container: com o mapping "/" do Spring Boot, o servletPath
        // é o path decodificado/normalizado — é ele que o filtro compara.
        request.setServletPath(uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
