package br.com.lumilivre.api.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames(
            "classpath:i18n/common/messages",
            "classpath:i18n/auth/messages",
            "classpath:i18n/reader/messages",
            "classpath:i18n/book/messages",
            "classpath:i18n/interest/messages",
            "classpath:i18n/loan/messages",
            "classpath:i18n/reservation/messages",
            "classpath:i18n/request/messages",
            "classpath:i18n/content/messages",
            "classpath:i18n/dashboard/messages",
            "classpath:i18n/validation/messages",
            "classpath:i18n/enum/messages",
            "classpath:i18n/course/messages",
            "classpath:i18n/user/messages",
            "classpath:i18n/email/messages",
            "classpath:i18n/import/messages",
            "classpath:i18n/report/messages",
            "classpath:i18n/settings/messages",
            "classpath:i18n/appversion/messages",
            "classpath:i18n/swagger/_api",
            "classpath:i18n/swagger/_common",
            "classpath:i18n/swagger/auth",
            "classpath:i18n/swagger/users",
            "classpath:i18n/swagger/readers",
            "classpath:i18n/swagger/books",
            "classpath:i18n/swagger/interest",
            "classpath:i18n/swagger/book-copies",
            "classpath:i18n/swagger/loans",
            "classpath:i18n/swagger/loan-requests",
            "classpath:i18n/swagger/reservations",
            "classpath:i18n/swagger/contents",
            "classpath:i18n/swagger/dashboard",
            "classpath:i18n/swagger/reports",
            "classpath:i18n/swagger/imports",
            "classpath:i18n/swagger/settings",
            "classpath:i18n/swagger/audit",
            "classpath:i18n/swagger/app-version",
            "classpath:i18n/swagger/metadata",
            "classpath:i18n/swagger/courses",
            "classpath:i18n/swagger/study-shifts",
            "classpath:i18n/swagger/academic-modules",
            "classpath:i18n/swagger/genres",
            "classpath:i18n/swagger/dewey-classifications",
            "classpath:i18n/swagger/system",
            "classpath:i18n/swagger/messages"
        );
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        source.setDefaultLocale(Locale.forLanguageTag("pt-BR"));
        source.setCacheSeconds(3600);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.forLanguageTag("pt-BR"));
        resolver.setSupportedLocales(List.of(
            Locale.forLanguageTag("pt-BR"),
            Locale.forLanguageTag("en-US")
        ));
        return resolver;
    }

    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }
}
