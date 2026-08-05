package br.com.lumilivre.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SEC-18 — a documentação OpenAPI não pode ser pública em produção.
 *
 * <p>O spec descreve toda rota, todo parâmetro e todo schema: é o mapa da API.
 * Mas ele também é a fonte dos clients gerados (orval no web, dart-dio no app),
 * que leem {@code /v3/api-docs} <b>sem token</b>. Daí o interruptor
 * {@code app.docs.public}: aberto em dev, ADMIN em produção
 * ({@code LUMILIVRE_DOCS_PUBLIC=false}).
 *
 * <p>Os dois lados são verificados porque errar qualquer um dos dois dói:
 * fechado por engano em dev quebra a regeneração dos clients; aberto por engano
 * em produção entrega o mapa.
 */
class DocsAccessTest {

    @Nested
    @SpringBootTest(properties = {
            "app.docs.public=true",
            "spring.flyway.enabled=false",
            "app.scheduling.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:lumilivre_docs_public;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=none",
            "jwt.secret=test-secret-key-with-enough-length-for-hmac-signature",
            "supabase.url=https://example.supabase.co",
            "supabase.key=test-key",
            "supabase.service-role.key=test-service-role-key",
            "app.cors.allowed-origins=http://localhost:5173"
    })
    @AutoConfigureMockMvc
    class WhenDocsArePublic {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void anonymousCanReadTheSpec() throws Exception {
            // É o que o `npm run api:gen` e o `generate_api.sh` fazem.
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest(properties = {
            "app.docs.public=false",
            "spring.flyway.enabled=false",
            "app.scheduling.enabled=false",
            "spring.datasource.url=jdbc:h2:mem:lumilivre_docs_admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.hibernate.ddl-auto=none",
            "jwt.secret=test-secret-key-with-enough-length-for-hmac-signature",
            "supabase.url=https://example.supabase.co",
            "supabase.key=test-key",
            "supabase.service-role.key=test-service-role-key",
            "app.cors.allowed-origins=http://localhost:5173"
    })
    @AutoConfigureMockMvc
    class WhenDocsAreRestricted {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void anonymousGetsUnauthorizedOnTheSpec() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        }

        @Test
        void anonymousGetsUnauthorizedOnTheSwaggerUi() throws Exception {
            mockMvc.perform(get("/docs")).andExpect(status().isUnauthorized());
            mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "LIBRARIAN")
        void librarianIsNotEnough() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminStillReadsTheSpec() throws Exception {
            mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        }

        @Test
        void healthStaysPublic() throws Exception {
            // Fechar as docs não pode arrastar o health check do compose/Render.
            // O status pode ser 503 aqui (sem SMTP nem Redis no teste); o que
            // importa é que a rota não passou a exigir autenticação.
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status == 401 || status == 403) {
                            throw new AssertionError("/actuator/health deixou de ser público: " + status);
                        }
                    });
        }
    }
}
