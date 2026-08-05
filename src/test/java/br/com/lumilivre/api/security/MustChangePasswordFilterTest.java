package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import jakarta.servlet.FilterChain;

/**
 * Com senha inicial = matrícula, a credencial é presumida comprometida: a flag
 * de troca obrigatória não pode liberar escrita nem PII de leitor.
 */
class MustChangePasswordFilterTest {

    // ObjectMapper pelo builder do Spring, MessageResolver com os bundles de
    // verdade e o LocaleResolver real: o corpo do 403 é exercitado, não simulado.
    private static final I18nConfig I18N = new I18nConfig();

    private final MustChangePasswordFilter filter = new MustChangePasswordFilter(
            Jackson2ObjectMapperBuilder.json().build(),
            new MessageResolver(I18N.messageSource()),
            I18N.localeResolver());
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
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
    void forbiddenBodyUsesTheStandardErrorEnvelope() throws Exception {
        // Este 403 era o único da API montado como string à mão: sem timestamp,
        // sem path, sem correlationId e com o texto cravado em inglês.
        authenticate(true);
        MDC.put("correlationId", "corr-123");
        MockHttpServletRequest request = request("POST", "/api/loan-requests");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding("UTF-8");

        filter.doFilter(request, response, chain);

        assertThat(response.getContentAsString())
                .contains("\"status\":403")
                .contains("\"timestamp\":")
                .contains("\"path\":\"/api/loan-requests\"")
                .contains("\"correlationId\":\"corr-123\"")
                // Contrato com o app: abrir o gate de senha, não deslogar.
                .contains("\"code\":\"PASSWORD_CHANGE_REQUIRED\"");
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    void forbiddenBodyIsLocalized() throws Exception {
        authenticate(true);

        MockHttpServletRequest ptRequest = request("POST", "/api/loan-requests");
        ptRequest.addPreferredLocale(Locale.forLanguageTag("pt-BR"));
        MockHttpServletResponse ptResponse = new MockHttpServletResponse();
        ptResponse.setCharacterEncoding("UTF-8");
        filter.doFilter(ptRequest, ptResponse, chain);

        assertThat(ptResponse.getHeader("Content-Language")).isEqualTo("pt-BR");
        assertThat(ptResponse.getContentAsString()).contains("Troque a senha inicial");

        MockHttpServletRequest enRequest = request("POST", "/api/loan-requests");
        enRequest.addPreferredLocale(Locale.forLanguageTag("en-US"));
        MockHttpServletResponse enResponse = new MockHttpServletResponse();
        enResponse.setCharacterEncoding("UTF-8");
        filter.doFilter(enRequest, enResponse, chain);

        assertThat(enResponse.getHeader("Content-Language")).isEqualTo("en-US");
        assertThat(enResponse.getContentAsString()).contains("Change the initial password");
    }

    /**
     * Este 403 é o primeiro que o aluno vê quando entra com a senha inicial, e o
     * filtro roda antes do DispatcherServlet — resolvia o locale sozinho, num
     * {@code if} de dois idiomas, então es/zh/hi saíam em português. O {@code code}
     * não é traduzido: é contrato com o app.
     */
    @ParameterizedTest(name = "Accept-Language {0} => 403 em {1}")
    @CsvSource({
        "es,    es, Cambia la contraseña inicial",
        "zh-CN, zh, 请先修改初始密码",
        "hi-IN, hi, शुरुआती पासवर्ड बदलें"
    })
    void forbiddenBodyIsLocalizedInEveryPublishedLanguage(
            String header, String expectedContentLanguage, String expectedFragment) throws Exception {
        authenticate(true);

        MockHttpServletRequest request = request("POST", "/api/loan-requests");
        request.addHeader("Accept-Language", header);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding("UTF-8");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getHeader("Content-Language")).isEqualTo(expectedContentLanguage);
        assertThat(response.getContentAsString())
            .contains(expectedFragment)
            .contains("\"code\":\"PASSWORD_CHANGE_REQUIRED\"");
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
