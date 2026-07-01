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

    @Value("${app.api.enabled:true}")
    private boolean apiEnabled;

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
                    if (!apiEnabled) {
                        auth.requestMatchers("/api/**").denyAll();
                    }
                    auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs", "/docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Public catalogue reads
                        .requestMatchers(HttpMethod.GET,
                                "/api/books/catalog",
                                "/api/books/public/search",
                                "/api/books/genres/**")
                                .permitAll()

                        // Mixed access by role
                        .requestMatchers(HttpMethod.GET,
                                "/api/readers/ranking",
                                "/api/loans/reader/**",
                                "/api/readers/{registrationNumber}",
                                "/api/books/recommendations/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

                        .requestMatchers(HttpMethod.GET, "/api/books/{id}")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

                        .requestMatchers(HttpMethod.PUT, "/api/loans/*/renew")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

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
                                "/api/study-shifts/**",
                                "/api/theses/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN", "READER")

                        // Reference data writes
                        .requestMatchers("/api/courses/**",
                                "/api/genres/**",
                                "/api/dewey-classifications/**",
                                "/api/academic-modules/**",
                                "/api/study-shifts/**",
                                "/api/theses/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        .requestMatchers("/api/dashboard/**", "/api/reports/**")
                                .hasAnyRole("ADMIN", "LIBRARIAN")

                        // Admin only
                        .requestMatchers(HttpMethod.GET, "/api/settings").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/settings").hasRole("ADMIN")
                        .requestMatchers("/api/imports/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

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
