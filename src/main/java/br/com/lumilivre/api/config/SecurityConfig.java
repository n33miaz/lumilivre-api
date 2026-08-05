package br.com.lumilivre.api.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import br.com.lumilivre.api.dto.common.ErrorResponse;
import br.com.lumilivre.api.enums.AccessEvent;
import br.com.lumilivre.api.security.AuthRateLimitFilter;
import br.com.lumilivre.api.security.CorrelationIdFilter;
import br.com.lumilivre.api.security.JwtAuthenticationFilter;
import br.com.lumilivre.api.security.MustChangePasswordFilter;
import br.com.lumilivre.api.service.AccessLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final MustChangePasswordFilter mustChangePasswordFilter;
    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;
    private final AccessLogService accessLogService;

    /** Rotas do springdoc: spec JSON, UI e o atalho /docs. */
    private static final String[] DOCS_PATHS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs", "/docs/**"
    };

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:8080}")
    private String[] allowedOrigins;

    @Value("${app.api.enabled:true}")
    private boolean apiEnabled;

    @Value("${app.docs.public:true}")
    private boolean docsPublic;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthRateLimitFilter authRateLimitFilter,
                          CorrelationIdFilter correlationIdFilter,
                          MustChangePasswordFilter mustChangePasswordFilter,
                          ObjectMapper objectMapper,
                          MessageResolver messageResolver,
                          AccessLogService accessLogService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.mustChangePasswordFilter = mustChangePasswordFilter;
        this.objectMapper = objectMapper;
        this.messageResolver = messageResolver;
        this.accessLogService = accessLogService;
    }

    @PostConstruct
    void validateCorsOrigins() {
        if (allowedOrigins == null || allowedOrigins.length == 0) {
            throw new ApplicationContextException(
                    "app.cors.allowed-origins is required and must declare at least one origin");
        }
        List<String> normalizedOrigins = new ArrayList<>();
        for (String origin : allowedOrigins) {
            if (origin == null || origin.isBlank()) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins contains a blank entry");
            }
            String trimmed = origin.trim();
            if (!origin.equals(trimmed) || trimmed.chars().anyMatch(Character::isWhitespace)) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins must not contain whitespace: " + trimmed);
            }
            if ("*".equals(trimmed)) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins cannot be '*' - credentials=true requires explicit origins");
            }
            URI uri;
            try {
                uri = URI.create(trimmed);
            } catch (IllegalArgumentException ex) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins must be absolute http(s) URLs: " + trimmed, ex);
            }
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins must be absolute http(s) URLs: " + trimmed);
            }
            if (uri.getHost() == null || uri.getUserInfo() != null) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins must declare only scheme, host and optional port: " + trimmed);
            }
            if (trimmed.endsWith("/")) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins must not end with '/': " + trimmed);
            }
            String path = uri.getRawPath();
            if ((path != null && !path.isEmpty()) || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new ApplicationContextException(
                        "app.cors.allowed-origins must not include path, query or fragment: " + trimmed);
            }
            normalizedOrigins.add(trimmed);
        }
        allowedOrigins = normalizedOrigins.toArray(String[]::new);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            Locale locale = resolveLocale(req);
                            ErrorResponse body = ErrorResponse.builder()
                                    .status(HttpStatus.UNAUTHORIZED.value())
                                    .error(messageResolver.resolve("error.unauthorized.title", locale))
                                    .message(messageResolver.resolve("error.unauthorized.message", locale))
                                    .path(req.getRequestURI())
                                    .correlationId(MDC.get("correlationId"))
                                    .build();
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.setHeader("Content-Language", locale.toLanguageTag());
                            objectMapper.writeValue(res.getWriter(), body);
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            logAccessDenied(e.getMessage());
                            Locale locale = resolveLocale(req);
                            ErrorResponse body = ErrorResponse.builder()
                                    .status(HttpStatus.FORBIDDEN.value())
                                    .error(messageResolver.resolve("error.access-denied.title", locale))
                                    .message(messageResolver.resolve("error.access-denied.message", locale))
                                    .path(req.getRequestURI())
                                    .correlationId(MDC.get("correlationId"))
                                    .build();
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json;charset=UTF-8");
                            res.setHeader("Content-Language", locale.toLanguageTag());
                            objectMapper.writeValue(res.getWriter(), body);
                        }))

                .authorizeHttpRequests(auth -> {
                    if (!apiEnabled) {
                        auth.requestMatchers("/api/**").denyAll();
                    }
                    auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        // Logout antes do permitAll de /api/auth/**: precisa de
                        // principal para saber de quem revogar os tokens.
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers("/api/auth/**").permitAll();

                    // Documentação OpenAPI (SEC-18): o spec é o mapa completo da
                    // API — toda rota, todo parâmetro, todo schema. Em dev fica
                    // aberto porque o orval (web) e o dart-dio (app) leem
                    // /v3/api-docs sem token para gerar os clients; com
                    // LUMILIVRE_DOCS_PUBLIC=false (produção) passa a exigir ADMIN,
                    // que é quem já pode ver tudo de qualquer forma.
                    if (docsPublic) {
                        auth.requestMatchers(DOCS_PATHS).permitAll();
                    } else {
                        auth.requestMatchers(DOCS_PATHS).hasRole("ADMIN");
                    }

                    auth
                        .requestMatchers("/actuator/health").permitAll()
                        // Métricas/infos operacionais só para ADMIN
                        .requestMatchers("/actuator/prometheus", "/actuator/info").hasRole("ADMIN")

                        // Public catalogue reads. A ficha do livro entra aqui: o
                        // catálogo já era anônimo e ela era o único ponto que
                        // exigia papel, o que o convidado do app via como erro
                        // de rede. BookResponse só carrega dado bibliográfico —
                        // ver o javadoc de BookController#getOne.
                        .requestMatchers(HttpMethod.GET,
                                "/api/books/catalog",
                                "/api/books/public/search",
                                "/api/books/genres/**",
                                "/api/books/{id}")
                                .permitAll()

                        // Recorte anônimo das configurações: sem ele o convidado
                        // não descobre que o modo convidado foi desligado.
                        .requestMatchers(HttpMethod.GET, "/api/settings/public").permitAll()

                        // App version check: app consulta antes de logar
                        .requestMatchers(HttpMethod.GET, "/api/app-version").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/app-version").hasRole("ADMIN")

                        // Mixed access by role
                        .requestMatchers(HttpMethod.GET,
                                "/api/readers/ranking",
                                "/api/loans/reader/**",
                                "/api/readers/{registrationNumber}",
                                "/api/books/recommendations/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

                        .requestMatchers(HttpMethod.PUT, "/api/loans/*/renew")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

                        // Content feed + single read available to readers (app mural)
                        .requestMatchers(HttpMethod.GET, "/api/contents/feed", "/api/contents/{id}")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")
                        // Content management (list/search/create/update/delete) is staff-only
                        .requestMatchers("/api/contents/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        .requestMatchers("/api/readers/**", "/api/books/**", "/api/loans/**",
                                "/api/book-copies/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        // Reader-accessible operational resources
                        .requestMatchers("/api/reservations/**", "/api/loan-requests/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

                        // Reference data reads
                        .requestMatchers(HttpMethod.GET,
                                "/api/courses/**",
                                "/api/genres/**",
                                "/api/metadata/**",
                                "/api/dewey-classifications/**",
                                "/api/academic-modules/**",
                                "/api/study-shifts/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

                        // Reference data writes
                        .requestMatchers("/api/courses/**",
                                "/api/genres/**",
                                "/api/dewey-classifications/**",
                                "/api/academic-modules/**",
                                "/api/study-shifts/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        .requestMatchers("/api/dashboard/**", "/api/reports/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        // Admin only
                        .requestMatchers(HttpMethod.GET, "/api/settings").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/settings").hasRole("ADMIN")
                        .requestMatchers("/api/imports/**").hasRole("ADMIN")
                        // Self-service do próprio usuário (ex.: concluir tour)
                        .requestMatchers("/api/users/me/**").authenticated()
                        // Ativar/bloquear conta é só do ADMIN (regra também no
                        // @PreAuthorize; aqui é a barreira de URL).
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/status").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        // Auditoria & acessos — leitura só ADMIN
                        .requestMatchers("/api/access-logs/**", "/api/audit-logs/**").hasRole("ADMIN")

                        .anyRequest().authenticated();
                })

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Após o JWT autenticar, bloqueia writes até a troca de senha obrigatória.
                .addFilterAfter(mustChangePasswordFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    private Locale resolveLocale(HttpServletRequest req) {
        Locale requested = req.getLocale();
        if ("en".equals(requested.getLanguage())) return Locale.forLanguageTag("en-US");
        return Locale.forLanguageTag("pt-BR");
    }

    /**
     * Registra a tentativa de acesso negado (403) na trilha de acessos, com o
     * ator resolvido pelo mesmo critério dos demais eventos (matrícula para
     * leitor) — a trilha só liga "negado" a "quem" se o nome for o mesmo.
     */
    private void logAccessDenied(String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = AccessLogService.actorOf(auth);
        accessLogService.record(AccessEvent.ACCESS_DENIED,
                actor != null ? actor : "anonymous",
                actor != null ? AccessLogService.roleOf(auth) : "ANONYMOUS",
                AccessLogService.RESULT_DENIED, message);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
