package br.com.lumilivre.api.service;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class EnumLabelResolver {

    private final MessageSource messageSource;

    public EnumLabelResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolve(Enum<?> e, Locale locale) {
        String key = "enum." + toKebabCase(e.getClass().getSimpleName()) + "." + e.name();
        return messageSource.getMessage(key, null, e.name(), locale);
    }

    private String toKebabCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
