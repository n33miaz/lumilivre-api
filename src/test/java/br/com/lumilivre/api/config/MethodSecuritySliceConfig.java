package br.com.lumilivre.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Liga a segurança de método dentro de uma fatia de {@code @WebMvcTest}.
 *
 * <p>Existe porque o slice <b>não</b> carrega o {@link SecurityConfig}, e é ele
 * que traz o {@code @EnableMethodSecurity}. Sem esta configuração os
 * {@code @PreAuthorize} dos controllers simplesmente não rodam: um teste que diz
 * "só ADMIN chega aqui" passa com qualquer papel, e passa também depois de
 * alguém apagar a anotação. Isso é pior que não ter teste — é teste verde
 * exatamente na camada de autorização, que é a que precisa de defesa.
 *
 * <p>A barreira por <i>URL</i> continua fora do slice (ela mora no
 * {@code SecurityConfig}); quem a verifica é o
 * {@link PublicEndpointsAccessTest}, com o contexto inteiro no ar.
 */
@TestConfiguration(proxyBeanMethods = false)
@EnableMethodSecurity
public class MethodSecuritySliceConfig {
}
