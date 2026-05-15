package br.com.lumilivre.api.config;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.MessageSource;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Locale;

public class SwaggerOperationCustomizer implements OperationCustomizer {

    private final MessageSource messageSource;
    private final Locale locale;

    public SwaggerOperationCustomizer(MessageSource messageSource, Locale locale) {
        this.messageSource = messageSource;
        this.locale = locale;
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation.getTags() == null) return operation;

        List<String> translatedTags = operation.getTags().stream()
                .map(tag -> {
                    String key = "swagger.tag." + tag.toLowerCase().replace(" ", "-") + ".name";
                    return messageSource.getMessage(key, null, tag, locale);
                })
                .toList();
        operation.setTags(translatedTags);
        return operation;
    }
}
