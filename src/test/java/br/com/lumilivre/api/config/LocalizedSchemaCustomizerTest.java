package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class LocalizedSchemaCustomizerTest {

    @Test
    void customiseLocalizesSchemasResponsesParametersAndHeaders() {
        Locale locale = Locale.US;
        StaticMessageSource messages = messages(locale);
        Components components = new Components()
                .addSchemas("BookRequest", new Schema<>()
                        .description("old schema")
                        .addProperty("title", new Schema<>().description("old title")))
                .addResponses("BadRequest", new ApiResponse()
                        .description("swagger.response.common.400.description"))
                .addParameters("AcceptLanguage", new Parameter()
                        .description("swagger.parameter.common.Accept-Language.description"))
                .addHeaders("Content-Language", new Header()
                        .description("swagger.header.Content-Language.description"));
        OpenAPI openApi = new OpenAPI().components(components);

        new LocalizedSchemaCustomizer(messages, locale).customise(openApi);

        assertThat(components.getSchemas().get("BookRequest").getDescription())
                .isEqualTo("Book request payload");
        assertThat(((Schema<?>) components.getSchemas().get("BookRequest")
                .getProperties().get("title")).getDescription())
                .isEqualTo("Book title");
        assertThat(components.getResponses().get("BadRequest").getDescription())
                .isEqualTo("Invalid request");
        assertThat(components.getParameters().get("AcceptLanguage").getDescription())
                .isEqualTo("Preferred response language");
        assertThat(components.getHeaders().get("Content-Language").getDescription())
                .isEqualTo("Resolved response language");
    }

    @Test
    void customiseLeavesComponentsNullSafe() {
        OpenAPI openApi = new OpenAPI();

        new LocalizedSchemaCustomizer(messages(Locale.US), Locale.US).customise(openApi);

        assertThat(openApi.getComponents()).isNull();
    }

    @Test
    void missingSchemaDescriptionFallsBackByLocale() {
        Components enComponents = new Components().addSchemas("Unknown", new Schema<>());
        Components ptComponents = new Components().addSchemas("Unknown", new Schema<>());

        new LocalizedSchemaCustomizer(new StaticMessageSource(), Locale.US)
                .customise(new OpenAPI().components(enComponents));
        new LocalizedSchemaCustomizer(new StaticMessageSource(), Locale.forLanguageTag("pt-BR"))
                .customise(new OpenAPI().components(ptComponents));

        assertThat(enComponents.getSchemas().get("Unknown").getDescription())
                .isEqualTo("Data contract for Unknown.");
        assertThat(ptComponents.getSchemas().get("Unknown").getDescription())
                .isEqualTo("Contrato de dados para Unknown.");
    }

    @Test
    void nonSwaggerComponentDescriptionsArePreserved() {
        Components components = new Components()
                .addResponses("Ok", new ApiResponse().description("Already localized"))
                .addParameters("Page", new Parameter().description("Page number"))
                .addHeaders("Trace", new Header().description("Trace id"));

        new LocalizedSchemaCustomizer(messages(Locale.US), Locale.US)
                .customise(new OpenAPI().components(components));

        assertThat(components.getResponses().get("Ok").getDescription()).isEqualTo("Already localized");
        assertThat(components.getParameters().get("Page").getDescription()).isEqualTo("Page number");
        assertThat(components.getHeaders().get("Trace").getDescription()).isEqualTo("Trace id");
    }

    private static StaticMessageSource messages(Locale locale) {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("swagger.schema.BookRequest.description", locale, "Book request payload");
        messages.addMessage("swagger.schema.BookRequest.title.description", locale, "Book title");
        messages.addMessage("swagger.response.common.400.description", locale, "Invalid request");
        messages.addMessage("swagger.parameter.common.Accept-Language.description", locale,
                "Preferred response language");
        messages.addMessage("swagger.header.Content-Language.description", locale, "Resolved response language");
        return messages;
    }
}
