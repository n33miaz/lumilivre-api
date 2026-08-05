package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;

/**
 * O filtro é onde a revogação acontece: assinatura válida não basta se a conta
 * perdeu o acesso ou se o token foi emitido antes do corte.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-with-enough-length-for-hmac-signature";
    private static final long EIGHT_HOURS = 28_800_000L;

    private JwtUtil jwtUtil;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtil = jwtUtil(EIGHT_HOURS);
        userDetailsService = mock(CustomUserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUsableAccountWithCurrentTokenVersion() throws Exception {
        AppUser appUser = appUser();
        String token = tokenFor(appUser);
        stubAccount(appUser);

        filter.doFilter(request(token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void revokesTokenImmediatelyWithNoDelayBetweenIssuingAndRevoking() throws Exception {
        // O caso que o corte por timestamp deixava passar: emitir e revogar no
        // mesmo instante. Com contador não existe granularidade para escapar.
        AppUser appUser = appUser();
        String token = tokenFor(appUser);

        appUser.revokeIssuedTokens();
        stubAccount(appUser);

        filter.doFilter(request(token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void tokenIssuedAfterTheRevocationIsAcceptedByConstruction() throws Exception {
        // É o token que o change-password devolve: emitido já com a versão nova,
        // no mesmo instante do incremento.
        AppUser appUser = appUser();
        appUser.revokeIssuedTokens();
        String replacement = tokenFor(appUser);
        stubAccount(appUser);

        filter.doFilter(request(replacement), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void everyRevocationInvalidatesTheTokenOfThePreviousGeneration() throws Exception {
        AppUser appUser = appUser();
        appUser.revokeIssuedTokens();
        String first = tokenFor(appUser);
        appUser.revokeIssuedTokens();
        String second = tokenFor(appUser);
        stubAccount(appUser);

        filter.doFilter(request(first), new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        filter.doFilter(request(second), new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void rejectsTokenWithoutTheVersionClaim() throws Exception {
        // Token emitido antes da V7: sem claim não há como conferir, falha fechado.
        AppUser appUser = appUser();
        String legacyToken = Jwts.builder()
                .subject(appUser.getEmail())
                .claim("roles", List.of("ROLE_LIBRARIAN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EIGHT_HOURS))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
        stubAccount(appUser);

        filter.doFilter(request(legacyToken), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsTokenWhoseVersionClaimIsNotNumeric() throws Exception {
        AppUser appUser = appUser();
        String tamperedToken = Jwts.builder()
                .subject(appUser.getEmail())
                .claim("roles", List.of("ROLE_LIBRARIAN"))
                .claim("tver", "nao-e-numero")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EIGHT_HOURS))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
        stubAccount(appUser);

        filter.doFilter(request(tamperedToken), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsInactiveAccount() throws Exception {
        AppUser appUser = appUser();
        appUser.setActive(false);
        stubAccount(appUser);

        filter.doFilter(request(tokenFor(appUser)), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsLockedAccount() throws Exception {
        AppUser appUser = appUser();
        appUser.setLocked(true);
        stubAccount(appUser);

        filter.doFilter(request(tokenFor(appUser)), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rejectsSoftDeletedAccount() throws Exception {
        AppUser appUser = appUser();
        appUser.setDeletedAt(OffsetDateTime.now());
        stubAccount(appUser);

        filter.doFilter(request(tokenFor(appUser)), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotBlowUpWhenAccountDisappearedAfterTokenWasIssued() throws Exception {
        AppUser appUser = appUser();
        String token = tokenFor(appUser);
        when(userDetailsService.loadUserByUsername(appUser.getEmail()))
                .thenThrow(new UsernameNotFoundException("gone"));

        filter.doFilter(request(token), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        AppUser appUser = appUser();
        // TTL negativo: token nasce vencido.
        String expired = tokenFor(appUser, jwtUtil(-1_000L));

        filter.doFilter(request(expired), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void requestWithoutTokenStaysAnonymous() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/api/books"),
                new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void stubAccount(AppUser appUser) {
        when(userDetailsService.loadUserByUsername(appUser.getEmail()))
                .thenReturn(new CustomUserDetails(appUser));
    }

    private String tokenFor(AppUser appUser) {
        return tokenFor(appUser, jwtUtil);
    }

    private String tokenFor(AppUser appUser, JwtUtil util) {
        return util.generateToken(
                new User(appUser.getEmail(), "hash",
                        List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()))),
                appUser.getTokenVersion());
    }

    private static MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static JwtUtil jwtUtil(long expirationMillis) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", SECRET);
        ReflectionTestUtils.setField(util, "expiration", expirationMillis);
        return util;
    }

    private static AppUser appUser() {
        return AppUser.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .email("librarian@lumilivre.test")
                .passwordHash("hash")
                .role(Role.LIBRARIAN)
                .build();
    }
}
