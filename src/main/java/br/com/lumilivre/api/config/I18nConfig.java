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
            "classpath:i18n/student/messages",
            "classpath:i18n/book/messages",
            "classpath:i18n/loan/messages",
            "classpath:i18n/reservation/messages",
            "classpath:i18n/request/messages",
            "classpath:i18n/thesis/messages",
            "classpath:i18n/dashboard/messages",
            "classpath:i18n/validation/messages",
            "classpath:i18n/enum/messages",
            "classpath:i18n/course/messages",
            "classpath:i18n/user/messages",
            "classpath:i18n/email/messages",
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
