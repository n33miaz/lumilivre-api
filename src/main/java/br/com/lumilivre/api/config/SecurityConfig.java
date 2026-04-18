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

import br.com.lumilivre.api.security.AuthRateLimitFilter;
import br.com.lumilivre.api.security.CorrelationIdFilter;
import br.com.lumilivre.api.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final CorrelationIdFilter correlationIdFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:8080}")
    private String[] allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          AuthRateLimitFilter authRateLimitFilter,
                          CorrelationIdFilter correlationIdFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.correlationIdFilter = correlationIdFilter;
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
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Access Denied\"}");
                        }))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints públicos
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Endpoints mobile de catálogo (GET público para leitura)
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
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.GET, "/livros/mobile/recomendacoes/**")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.GET, "/livros/{id}")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.GET, "/emprestimos/aluno/**")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.GET, "/solicitacoes/aluno/**")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.GET, "/alunos/{matricula}")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.POST,
                                "/solicitacoes/solicitar",
                                "/solicitacoes/solicitar-mobile")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.POST, "/reservas")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.DELETE, "/reservas/*/cancelar")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.PUT, "/emprestimos/renovar/**")
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        .requestMatchers(HttpMethod.PUT, "/usuarios/alterar-senha")
                                .authenticated()

                        // Admin exclusivo
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

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
                                .hasAnyRole("ADMIN", "BIBLIOTECARIO")

                        .anyRequest().authenticated())

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
