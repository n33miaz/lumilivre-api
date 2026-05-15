package br.com.lumilivre.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final MessageSource messageSource;

    @Bean
    public GroupedOpenApi v2PtBrGroup() {
        return GroupedOpenApi.builder()
                .group("v2-pt-br")
                .displayName("API v2 — PT-BR")
                .pathsToMatch("/api/v2/**")
                .addOperationCustomizer(new SwaggerOperationCustomizer(messageSource, Locale.forLanguageTag("pt-BR")))
                .build();
    }

    @Bean
    public GroupedOpenApi v2EnUsGroup() {
        return GroupedOpenApi.builder()
                .group("v2-en-us")
                .displayName("API v2 — EN-US")
                .pathsToMatch("/api/v2/**")
                .addOperationCustomizer(new SwaggerOperationCustomizer(messageSource, Locale.forLanguageTag("en-US")))
                .build();
    }

    @Bean
    public GroupedOpenApi v1LegacyGroup() {
        return GroupedOpenApi.builder()
                .group("v1-legacy")
                .displayName("API v1 — Legacy (PT-BR)")
                .pathsToExclude("/api/v2/**", "/actuator/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .tags(buildTags())
                .servers(buildServers())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(buildComponents());
    }

    private Info buildInfo() {
        String ptDesc = messageSource.getMessage("swagger.api.description", null, "", Locale.forLanguageTag("pt-BR"));
        String enDesc = messageSource.getMessage("swagger.api.description", null, "", Locale.forLanguageTag("en-US"));
        return new Info()
                .title("LumiLivre API")
                .version("v2.0")
                .description(ptDesc + "\n\n---\n\n" + enDesc)
                .contact(new Contact()
                        .name("LumiLivre")
                        .email("contato.lumilivre@gmail.com"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"));
    }

    private List<Tag> buildTags() {
        Locale pt = Locale.forLanguageTag("pt-BR");
        return List.of(
                tag("auth", pt),
                tag("students", pt),
                tag("books", pt),
                tag("loans", pt),
                tag("loan-requests", pt),
                tag("reservations", pt),
                tag("courses", pt),
                tag("study-shifts", pt),
                tag("academic-modules", pt),
                tag("genres", pt),
                tag("dewey-classifications", pt),
                tag("theses", pt),
                tag("dashboard", pt),
                tag("reports", pt),
                tag("imports", pt),
                tag("users", pt)
        );
    }

    private Tag tag(String key, Locale locale) {
        String name = messageSource.getMessage("swagger.tag." + key + ".name", null, key, locale);
        String desc = messageSource.getMessage("swagger.tag." + key + ".description", null, "", locale);
        return new Tag().name(name).description(desc);
    }

    private List<Server> buildServers() {
        return List.of(
                new Server().url("http://localhost:8080").description("Development"),
                new Server().url("https://lumilivre-api.onrender.com").description("Production")
        );
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("JWT token returned by POST /api/v2/auth/login. Insert only the value (without 'Bearer ')."))
                .addRequestBodies("LoginRequest", buildLoginRequestBody());
    }

    private RequestBody buildLoginRequestBody() {
        return new RequestBody()
                .description("Login credentials / Credenciais de login")
                .required(true)
                .content(new Content().addMediaType("application/json",
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/LoginRequest"))
                                .addExamples("admin-en", new Example()
                                        .summary("Admin/Librarian (EN)")
                                        .value("{\"username\":\"admin@lumilivre.com.br\",\"password\":\"senha123\"}"))
                                .addExamples("student-en", new Example()
                                        .summary("Student (EN)")
                                        .value("{\"username\":\"2024001\",\"password\":\"2024001\"}"))));
    }
}
