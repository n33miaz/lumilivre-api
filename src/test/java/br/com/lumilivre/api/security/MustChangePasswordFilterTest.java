package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import jakarta.servlet.FilterChain;

/**
 * Com senha inicial = matrícula, a credencial é presumida comprometida: a flag
 * de troca obrigatória não pode liberar escrita nem PII de leitor.
 */
class MustChangePasswordFilterTest {

    private final MustChangePasswordFilter filter = new MustChangePasswordFilter();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksWritesWhilePasswordChangeIsPending() throws Exception {
        authenticate(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/api/loan-requests"), response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("PASSWORD_CHANGE_REQUIRED");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void blocksReaderPersonalDataReadsWhilePasswordChangeIsPending() throws Exception {
        authenticate(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/readers/2024001"), response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsRankingBecauseItCarriesNoPersonalData() throws Exception {
        authenticate(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/readers/ranking"), response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(chain).doFilter(any(), any());
    }

    @Test
    void allowsBootstrapReadsSoTheClientCanShowTheChangePasswordGate() throws Exception {
        authenticate(true);

        filter.doFilter(request("GET", "/api/settings"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("GET", "/api/books/catalog"), new MockHttpServletResponse(), chain);

        verify(chain, org.mockito.Mockito.times(2)).doFilter(any(), any());
    }

    @Test
    void allowsTheChangePasswordCallItself() throws Exception {
        authenticate(true);

        filter.doFilter(request("PUT", "/api/auth/change-password"), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void doesNothingWhenPasswordIsAlreadySettled() throws Exception {
        authenticate(false);

        filter.doFilter(request("GET", "/api/readers/2024001"), new MockHttpServletResponse(), chain);
        filter.doFilter(request("POST", "/api/loan-requests"), new MockHttpServletResponse(), chain);

        verify(chain, org.mockito.Mockito.times(2)).doFilter(any(), any());
    }

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static void authenticate(boolean mustChangePassword) {
        AppUser appUser = AppUser.builder()
                .email("reader@lumilivre.test")
                .passwordHash("hash")
                .role(Role.READER)
                .mustChangePassword(mustChangePassword)
                .build();
        CustomUserDetails principal = new CustomUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
