package br.com.lumilivre.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Map;

public class LocalizedSchemaCustomizer implements OpenApiCustomizer {

    private final MessageSource messages;
    private final Locale locale;

    public LocalizedSchemaCustomizer(MessageSource messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    @Override
    public void customise(OpenAPI openApi) {
        Components components = openApi.getComponents();
        if (components == null) {
            return;
        }
        localizeSchemas(components);
        localizeResponses(components);
        localizeParameters(components);
        localizeHeaders(components);
    }

    private void localizeSchemas(Components components) {
        if (components.getSchemas() == null) {
            return;
        }
        components.getSchemas().forEach((name, schema) -> {
            schema.setDescription(resolve(
                    "swagger.schema." + name + ".description",
                    fallbackSchemaDescription(name)));
            Map<String, Schema> properties = schema.getProperties();
            if (properties == null) {
                return;
            }
            properties.forEach((field, fieldSchema) -> fieldSchema.setDescription(resolve(
                    "swagger.schema." + name + "." + field + ".description",
                    fieldSchema.getDescription())));
        });
    }

    private void localizeResponses(Components components) {
        if (components.getResponses() == null) {
            return;
        }
        components.getResponses().forEach((name, response) -> localizeResponse(name, response));
    }

    private void localizeResponse(String name, ApiResponse response) {
        if (response.getDescription() == null || !response.getDescription().startsWith("swagger.")) {
            return;
        }
        response.setDescription(resolve(response.getDescription(), name));
    }

    private void localizeParameters(Components components) {
        if (components.getParameters() == null) {
            return;
        }
        components.getParameters().forEach((name, parameter) -> localizeParameter(name, parameter));
    }

    private void localizeParameter(String name, Parameter parameter) {
        if (parameter.getDescription() == null || !parameter.getDescription().startsWith("swagger.")) {
            return;
        }
        parameter.setDescription(resolve(parameter.getDescription(), name));
    }

    private void localizeHeaders(Components components) {
        if (components.getHeaders() == null) {
            return;
        }
        components.getHeaders().forEach((name, header) -> localizeHeader(name, header));
    }

    private void localizeHeader(String name, Header header) {
        if (header.getDescription() == null || !header.getDescription().startsWith("swagger.")) {
            return;
        }
        header.setDescription(resolve(header.getDescription(), name));
    }

    private String fallbackSchemaDescription(String schemaName) {
        if ("pt".equals(locale.getLanguage())) {
            return "Contrato de dados para " + schemaName + ".";
        }
        return "Data contract for " + schemaName + ".";
    }

    private String resolve(String key, String fallback) {
        return messages.getMessage(key, null, fallback != null ? fallback : "", locale);
    }
}
