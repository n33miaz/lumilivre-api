package br.com.lumilivre.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;

public class LocalizedTagsCustomizer implements OpenApiCustomizer {

    private final MessageSource messages;
    private final Locale locale;
    private final List<String> tagKeys;

    public LocalizedTagsCustomizer(MessageSource messages, Locale locale, List<String> tagKeys) {
        this.messages = messages;
        this.locale = locale;
        this.tagKeys = tagKeys;
    }

    @Override
    public void customise(OpenAPI openApi) {
        List<Tag> tags = tagKeys.stream()
                .map(this::tag)
                .toList();
        openApi.setTags(tags);
    }

    private Tag tag(String key) {
        return new Tag()
                .name(resolve("swagger.tag." + key + ".name", key))
                .description(resolve("swagger.tag." + key + ".description", ""));
    }

    private String resolve(String key, String fallback) {
        return messages.getMessage(key, null, fallback, locale);
    }
}
