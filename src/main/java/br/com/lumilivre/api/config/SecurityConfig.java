package br.com.lumilivre.api.config;

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

import br.com.lumilivre.api.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Ativa o CORS do Spring Security (obrigatório!)
                .cors(cors -> {
                })
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
                        // libera
                        .requestMatchers("/error").permitAll()

                        // rotas publicas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        .requestMatchers(HttpMethod.PUT, "/usuarios/alterar-senha").authenticated()
                        // rotas mobile GET
                        .requestMatchers(HttpMethod.GET,
                                "/livros/catalogo-mobile",
                                "/livros/{id}",
                                "/livros/genero/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/emprestimos/ranking",
                                "/cursos/home",
                                "/modulos/home",
                                "/turnos/home",
                                "/alunos/{matricula}"
                        ).hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")
                        .requestMatchers(HttpMethod.GET, "/emprestimos/aluno/**")
                        .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")
                        .requestMatchers(HttpMethod.GET, "/solicitacoes/aluno/**")
                        .hasAnyRole("ADMIN", "BIBLIOTECARIO", "ALUNO")

                        // rotas de ADMIN
                        .requestMatchers("/usuarios/**").hasRole("ADMIN")

                        // ADMIN ou BIBLIOTECARIO
                        .requestMatchers(
                                "/livros/**",
                                "/tcc/**",
                                "/generos/**",
                                "/autores/**",
                                "/cursos/**",
                                "/emprestimos/**",
                                "/alunos/**")
                        .hasAnyRole("ADMIN", "BIBLIOTECARIO")

                        .anyRequest().authenticated())

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

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
                        .allowedOrigins(
                                "https://www.lumilivre.com.br", // produção
                                "http://localhost:5173", // desenv. web
                                "http://localhost:58967", "http://192.168.56.1:8080", "http://127.0.0.1:8080", "http://localhost:8080" // desenv. mobile
                )
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}