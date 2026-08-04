package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * SEC-02/SEC-04: o filtro deve throttlar os endpoints REAIS de auth
 * (/api/auth/**) por IP resolvido pelo proxy — e NÃO deve confiar em
 * X-Forwarded-For do cliente (falsificável).
 */
class AuthRateLimitFilterTest {

    @Test
    void authEndpointsAllowFiveRequestsAndRejectTheSixthForSameIp() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
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
        assertThat(blocked.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void forwardedForHeaderIsIgnoredForRateLimiting() throws Exception {
        // Mesmo IP resolvido pelo proxy (remoteAddr) com X-Forwarded-For variável
        // NÃO deve burlar o limite: o filtro ignora o header do cliente (SEC-04).
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
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
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
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
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
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
    void blockedRequestDoesNotContinueFilterChain() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
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
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
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

    private static MockHttpServletRequest authRequest(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        // Espelha o container: com o mapping "/" do Spring Boot, o servletPath
        // é o path decodificado/normalizado — é ele que o filtro compara.
        request.setServletPath(uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
