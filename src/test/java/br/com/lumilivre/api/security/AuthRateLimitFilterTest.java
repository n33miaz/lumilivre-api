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
    void forwardedForHeaderUsesFirstClientIp() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(forwardedLoginRequest("198.51.100.7, 10.0.0.1"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(forwardedLoginRequest("198.51.100.7, 10.0.0.2"), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void nonAuthEndpointsBypassRateLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        request.setServletPath("/api/books");
        request.setRemoteAddr("203.0.113.20");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void authPasswordResetEndpointUsesSameLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter();
        FilterChain chain = org.mockito.Mockito.mock(FilterChain.class);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(authRequest("/auth/esqueci-senha", "203.0.113.30"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(authRequest("/auth/esqueci-senha", "203.0.113.30"), blocked, chain);

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
        return authRequest("/auth/login", remoteAddr);
    }

    private static MockHttpServletRequest forwardedLoginRequest(String forwardedFor) {
        MockHttpServletRequest request = loginRequest("10.0.0.99");
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    private static MockHttpServletRequest authRequest(String path, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
