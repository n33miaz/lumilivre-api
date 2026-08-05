package br.com.lumilivre.api.config;

import java.util.ArrayList;
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

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final Locale EN_US = Locale.forLanguageTag("en-US");

    /**
     * Idiomas publicados. Espanhol, mandarim e hindi entram sem pais no tag de
     * proposito: o bundle e um so por idioma, entao {@code es-MX}, {@code zh-TW} e
     * {@code hi-IN} casam pelo idioma em vez de cair no padrao — o
     * {@code AcceptHeaderLocaleResolver} so faz esse casamento por idioma contra
     * um locale suportado sem pais.
     *
     * <p>Portugues e ingles mantem o pais porque os arquivos sao {@code _pt_BR} e
     * {@code _en_US}, mas {@code en} entra tambem: sem ele um {@code en-GB} cai
     * no portugues. O {@code en} generico funciona porque a cadeia de fallback
     * abaixo passa por {@code _en_US}. O mesmo truque nao serve para {@code pt}
     * generico — ele sairia em ingles pela cadeia —, e {@code pt-PT} ja resolve
     * bem pelo locale padrao.
     */
    private static final List<Locale> SUPPORTED_LOCALES = List.of(
        PT_BR,
        EN_US,
        Locale.forLanguageTag("en"),
        Locale.forLanguageTag("es"),
        Locale.forLanguageTag("zh"),
        Locale.forLanguageTag("hi")
    );

    /**
     * Ordem de fallback quando a chave nao existe no idioma pedido: ingles antes
     * de portugues. Com cinco idiomas, o {@code defaultLocale} unico do Spring
     * jogaria uma chave faltante em mandarim direto para o portugues; ingles no
     * meio da cadeia e o menor prejuizo para quem pediu zh ou hi. O guard de
     * paridade (scripts/check-i18n-coverage.sh) existe justamente para essa
     * cadeia nunca ser exercitada em producao.
     */
    private static final List<Locale> FALLBACK_CHAIN = List.of(EN_US, PT_BR);

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new FallbackChainMessageSource();
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
        source.setDefaultLocale(PT_BR);
        source.setCacheSeconds(3600);
        return source;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(PT_BR);
        resolver.setSupportedLocales(SUPPORTED_LOCALES);
        return resolver;
    }

    @Bean
    public LocalValidatorFactoryBean getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

    /**
     * Aplica o {@link #FALLBACK_CHAIN} na busca dos arquivos de bundle.
     *
     * <p>O comportamento padrao tenta o locale pedido e depois o
     * {@code defaultLocale} — um degrau so. Aqui a lista de candidatos e montada
     * inteira, na ordem da cadeia, para que a degradacao seja previsivel e
     * documentada em vez de emergente.
     */
    private static class FallbackChainMessageSource extends ReloadableResourceBundleMessageSource {

        @Override
        protected List<String> calculateAllFilenames(String basename, Locale locale) {
            List<String> filenames = new ArrayList<>(calculateFilenamesForLocale(basename, locale));
            for (Locale fallback : FALLBACK_CHAIN) {
                for (String filename : calculateFilenamesForLocale(basename, fallback)) {
                    if (!filenames.contains(filename)) {
                        filenames.add(filename);
                    }
                }
            }
            if (!filenames.contains(basename)) {
                filenames.add(basename);
            }
            return filenames;
        }
    }
}
