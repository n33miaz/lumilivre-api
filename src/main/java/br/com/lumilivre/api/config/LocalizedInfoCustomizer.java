package br.com.lumilivre.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.MessageSource;

import java.util.Locale;

public class LocalizedInfoCustomizer implements OpenApiCustomizer {

    private final MessageSource messages;
    private final Locale locale;

    public LocalizedInfoCustomizer(MessageSource messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    @Override
    public void customise(OpenAPI openApi) {
        Info current = openApi.getInfo() != null ? openApi.getInfo() : new Info();
        openApi.setInfo(current
                .title(resolve("swagger.api.title", "LumiLivre API"))
                .version(resolve("swagger.api.version", "1.0.0"))
                .description(resolve("swagger.api.description", ""))
                .termsOfService(resolve("swagger.api.terms", null))
                .contact(new Contact()
                        .name(resolve("swagger.api.contact.name", "LumiLivre"))
                        .email(resolve("swagger.api.contact.email", "contato.lumilivre@gmail.com"))
                        .url(resolve("swagger.api.contact.url", "https://github.com/n33miaz/lumilivre")))
                .license(new License()
                        .name(resolve("swagger.api.license.name", "MIT"))
                        .url(resolve("swagger.api.license.url", "https://opensource.org/licenses/MIT"))));
    }

    private String resolve(String key, String fallback) {
        return messages.getMessage(key, null, fallback, locale);
    }
}
