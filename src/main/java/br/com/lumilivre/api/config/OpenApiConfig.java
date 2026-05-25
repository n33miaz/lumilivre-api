package br.com.lumilivre.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
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
    public GroupedOpenApi apiPtBrGroup() {
        return localizedGroup("api-pt-br", "API - Portugues (Brasil)", Locale.forLanguageTag("pt-BR"));
    }

    @Bean
    public GroupedOpenApi apiEnUsGroup() {
        return localizedGroup("api-en-us", "API - English (US)", Locale.forLanguageTag("en-US"));
    }

    @Bean
    public GroupedOpenApi systemGroup() {
        return GroupedOpenApi.builder()
                .group("system")
                .displayName("System")
                .pathsToMatch("/", "/actuator/**")
                .addOpenApiCustomizer(new LocalizedInfoCustomizer(messageSource, Locale.forLanguageTag("en-US")))
                .addOpenApiCustomizer(new LocalizedTagsCustomizer(messageSource, Locale.forLanguageTag("en-US"), SwaggerTags.SYSTEM_TAGS))
                .addOpenApiCustomizer(new LocalizedSchemaCustomizer(messageSource, Locale.forLanguageTag("en-US")))
                .addOperationCustomizer(new LocalizedOperationCustomizer(messageSource, Locale.forLanguageTag("en-US")))
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .servers(buildServers())
                .components(buildComponents());
    }

    private GroupedOpenApi localizedGroup(String groupId, String displayName, Locale locale) {
        return GroupedOpenApi.builder()
                .group(groupId)
                .displayName(displayName)
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(new LocalizedInfoCustomizer(messageSource, locale))
                .addOpenApiCustomizer(new LocalizedTagsCustomizer(messageSource, locale, SwaggerTags.API_TAGS))
                .addOpenApiCustomizer(new LocalizedSchemaCustomizer(messageSource, locale))
                .addOperationCustomizer(new LocalizedOperationCustomizer(messageSource, locale))
                .build();
    }

    private Info buildInfo() {
        return new Info()
                .title(messageSource.getMessage("swagger.api.title", null, "LumiLivre API", Locale.forLanguageTag("pt-BR")))
                .version(messageSource.getMessage("swagger.api.version", null, "1.0.0", Locale.forLanguageTag("pt-BR")))
                .description(messageSource.getMessage("swagger.api.description", null, "", Locale.forLanguageTag("pt-BR")))
                .contact(new Contact()
                        .name("LumiLivre")
                        .email("ncormino@gmail.com"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"));
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
                        .description("JWT token returned by POST /api/auth/login. Insert only the value (without 'Bearer ')."))
                .addResponses("BadRequest", response("swagger.response.common.400.description"))
                .addResponses("Unauthorized", response("swagger.response.common.401.description"))
                .addResponses("Forbidden", response("swagger.response.common.403.description"))
                .addResponses("NotFound", response("swagger.response.common.404.description"))
                .addResponses("Conflict", response("swagger.response.common.409.description"))
                .addResponses("BusinessRuleViolation", response("swagger.response.common.422.description"))
                .addResponses("RateLimited", response("swagger.response.common.429.description"))
                .addResponses("ServerError", response("swagger.response.common.500.description"))
                .addHeaders("Content-Language", new Header()
                        .description("swagger.header.content-language.description")
                        .schema(new StringSchema().example("pt-BR")))
                .addHeaders("X-Correlation-Id", new Header()
                        .description("swagger.header.x-correlation-id.description")
                        .schema(new StringSchema().example("a1b2c3d4-e5f6-7890")))
                .addParameters("AcceptLanguage", new HeaderParameter()
                        .name("Accept-Language")
                        .required(false)
                        .description("swagger.parameter.common.locale.description")
                        .schema(new StringSchema()._enum(List.of("pt-BR", "en-US")).example("pt-BR")));
    }

    private ApiResponse response(String descriptionKey) {
        return new ApiResponse()
                .description(descriptionKey)
                .addHeaderObject("Content-Language", new Header().$ref("#/components/headers/Content-Language"))
                .addHeaderObject("X-Correlation-Id", new Header().$ref("#/components/headers/X-Correlation-Id"))
                .content(new Content().addMediaType("application/json",
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                .addExamples("default", new Example()
                                        .value("{\"timestamp\":\"2026-05-22T10:00:00\",\"status\":400,"
                                                + "\"error\":\"Bad Request\",\"message\":\"Invalid request\","
                                                + "\"path\":\"/api/books\",\"correlationId\":\"a1b2c3d4\"}"))));
    }
}
