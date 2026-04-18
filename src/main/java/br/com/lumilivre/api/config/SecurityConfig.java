package br.com.lumilivre.api.config;

import org.springframework.beans.factory.annotation.Value;
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
