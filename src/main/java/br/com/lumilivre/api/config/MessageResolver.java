package br.com.lumilivre.api.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageResolver {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);

    private final MessageSource messageSource;

    public MessageResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String resolve(String key, Object... args) {
        return resolve(key, LocaleContextHolder.getLocale(), args);
    }

    public String resolve(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, localizeDates(args, locale), key, locale);
    }

    /**
     * Data em argumento sai no formato do locale, nunca em ISO.
     *
     * <p>O {@code MessageFormat} nao sabe formatar {@code java.time}: cai no
     * {@code toString()}, e a penalidade do leitor chegava ao aluno como
     * "2026-08-11" — texto de log no meio de uma frase. Converter aqui, no unico
     * ponto por onde toda mensagem traduzida passa, alinha a data ao idioma que o
     * cliente pediu sem espalhar formatador pelos services nem obrigar cada
     * chamada a saber o locale efetivo.
     */
    private static Object[] localizeDates(Object[] args, Locale locale) {
        if (args == null || args.length == 0 || locale == null) {
            return args;
        }
        Object[] localized = null;
        for (int i = 0; i < args.length; i++) {
            String formatted = formatDate(args[i], locale);
            if (formatted == null) {
                continue;
            }
            if (localized == null) {
                localized = args.clone();
            }
            localized[i] = formatted;
        }
        return localized != null ? localized : args;
    }

    /** Devolve {@code null} para o que nao e data, sinalizando "deixe como esta". */
    private static String formatDate(Object arg, Locale locale) {
        if (arg instanceof LocalDate date) {
            return DATE.withLocale(locale).format(date);
        }
        // Campos com hora mantem a hora: perder informacao seria pior que o ISO.
        if (arg instanceof OffsetDateTime dateTime) {
            return DATE_TIME.withLocale(locale).format(dateTime);
        }
        if (arg instanceof ZonedDateTime dateTime) {
            return DATE_TIME.withLocale(locale).format(dateTime);
        }
        if (arg instanceof LocalDateTime dateTime) {
            return DATE_TIME.withLocale(locale).format(dateTime);
        }
        return null;
    }
}
