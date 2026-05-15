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
import br.com.lumilivre.api.security.AuthRateLimitFilter;
import br.com.lumilivre.api.security.CorrelationIdFilter;
import br.com.lumilivre.api.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final CorrelationIdFilter correlationIdFilter;
    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:8080}")
    private String[] allowedOrigins;

    @Value("${app.api.v2.enabled:true}")
    private boolean v2ApiEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthRateLimitFilter authRateLimitFilter,
                          CorrelationIdFilter correlationIdFilter,
                          ObjectMapper objectMapper,
                          MessageResolver messageResolver) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.correlationIdFilter = correlationIdFilter;
        this.objectMapper = objectMapper;
        this.messageResolver = messageResolver;
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
                    if (!v2ApiEnabled) {
                        auth.requestMatchers("/api/v2/**").denyAll();
                    }
                    auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints públicos
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/v2/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Endpoints mobile de catálogo (GET público para leitura)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v2/books/catalog",
                                "/api/v2/books/public/search",
                                "/api/v2/books/genres/**")
                                .permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/livros/catalogo-mobile",
                                "/livros/mobile/buscar",
                                "/livros/genero/**").permitAll()

                        // Recursos autenticados com acesso a alunos/empréstimos/solicitações por role
                        .requestMatchers(HttpMethod.GET,
                                "/emprestimos/ranking",
                                "/cursos/home",
                                "/modulos/home",
                                "/turnos/home")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/livros/mobile/recomendacoes/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/livros/{id}")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/emprestimos/aluno/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/solicitacoes/aluno/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/alunos/{matricula}")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.POST,
                                "/solicitacoes/solicitar",
                                "/solicitacoes/solicitar-mobile")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.POST, "/reservas")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.DELETE, "/reservas/*/cancelar")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.PUT, "/emprestimos/renovar/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.PUT, "/usuarios/alterar-senha")
                                .authenticated()

                        // Admin exclusivo
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

                        // v2 — acesso misto por role (lógica idêntica à v1)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v2/students/ranking",
                                "/api/v2/loans/student/**",
                                "/api/v2/students/{registrationNumber}",
                                "/api/v2/books/recommendations/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.GET, "/api/v2/books/{id}")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers(HttpMethod.PUT, "/api/v2/loans/*/renew")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        .requestMatchers("/api/v2/students/**", "/api/v2/books/**", "/api/v2/loans/**",
                                "/api/v2/book-copies/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        // v2 — Acesso estudante: reservas, solicitações, referência
                        .requestMatchers("/api/v2/reservations/**", "/api/v2/loan-requests/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        // v2 — Dados de referência (leitura liberada para autenticados)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v2/courses/**",
                                "/api/v2/genres/**",
                                "/api/v2/metadata/**",
                                "/api/v2/dewey-classifications/**",
                                "/api/v2/academic-modules/**",
                                "/api/v2/study-shifts/**",
                                "/api/v2/theses/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "STUDENT")

                        // v2 — Escrita de referência requer ADMIN/LIBRARIAN
                        .requestMatchers("/api/v2/courses/**",
                                "/api/v2/genres/**",
                                "/api/v2/dewey-classifications/**",
                                "/api/v2/academic-modules/**",
                                "/api/v2/study-shifts/**",
                                "/api/v2/theses/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        .requestMatchers("/api/v2/dashboard/**", "/api/v2/reports/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        // v2 — Admin exclusivo
                        .requestMatchers("/api/v2/imports/**").hasRole("ADMIN")
                        .requestMatchers("/api/v2/users/**").hasRole("ADMIN")

                        // Admin ou Bibliotecário
                        .requestMatchers(
                                "/livros/**",
                                "/tcc/**",
                                "/generos/**",
                                "/cdds/**",
                                "/cursos/**",
                                "/modulos/**",
                                "/turnos/**",
                                "/exemplares/**",
                                "/emprestimos/**",
                                "/solicitacoes/**",
                                "/alunos/**",
                                "/relatorios/**",
                                "/importacao/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        .anyRequest().authenticated();
                })

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
