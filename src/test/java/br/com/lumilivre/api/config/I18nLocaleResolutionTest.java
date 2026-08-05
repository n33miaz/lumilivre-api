package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Resolucao de locale e cadeia de fallback dos bundles.
 *
 * <p>Cobre o que o cliente consegue mandar no {@code Accept-Language} e que
 * nenhum valor — variante regional ou tag inexistente — pode virar erro.
 */
class I18nLocaleResolutionTest {

    private final I18nConfig config = new I18nConfig();

    @ParameterizedTest(name = "Accept-Language: {0} => {1}")
    @CsvSource({
        "pt-BR, pt-BR",
        "en-US, en-US",
        "es,    es",
        "zh,    zh",
        "hi,    hi",
        // Variantes regionais casam pelo idioma: o bundle e um so por idioma.
        "es-MX, es",
        "zh-CN, zh",
        "zh-TW, zh",
        "hi-IN, hi",
        // Ingles generico existe na lista de suportados para que en-GB nao caia
        // no portugues; o conteudo vem de _en_US pela cadeia de fallback.
        "en,    en",
        "en-GB, en",
        // Portugues generico nao esta na lista: pt-PT resolve pelo locale padrao.
        "pt-PT, pt-BR",
        // Tag inexistente cai no padrao, nunca em erro.
        "xx-YY, pt-BR"
    })
    void resolvesAcceptLanguageToASupportedLocale(String header, String expectedTag) {
        LocaleResolver resolver = config.localeResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", header);

        assertThat(resolver.resolveLocale(request).toLanguageTag()).isEqualTo(expectedTag);
    }

    @Test
    void missingHeaderResolvesToPtBr() {
        LocaleResolver resolver = config.localeResolver();

        assertThat(resolver.resolveLocale(new MockHttpServletRequest()).toLanguageTag())
            .isEqualTo("pt-BR");
    }

    @ParameterizedTest(name = "locale={0} resolve mensagem de negocio")
    @CsvSource({
        "pt-BR, Livro não encontrado.",
        "en-US, Book not found.",
        "es, Libro no encontrado.",
        "zh, 未找到图书。",
        "hi, किताब नहीं मिली।"
    })
    void businessMessagesExistInEveryPublishedLocale(String tag, String expected) {
        MessageSource source = config.messageSource();

        assertThat(source.getMessage("book.not-found", null, Locale.forLanguageTag(tag)))
            .isEqualTo(expected);
    }

    /**
     * Os bundles de swagger existem so em pt-BR e en-US, entao servem de prova da
     * ordem da cadeia: pedindo espanhol tem que sair ingles, nao portugues.
     */
    @Test
    void keyAbsentFromTheRequestedLocaleFallsBackToEnglishBeforePortuguese() {
        MessageSource source = config.messageSource();
        String english = source.getMessage("swagger.tag.auth.name", null, Locale.forLanguageTag("en-US"));
        String portuguese = source.getMessage("swagger.tag.auth.name", null, Locale.forLanguageTag("pt-BR"));

        for (String tag : new String[] { "es", "zh", "hi" }) {
            assertThat(source.getMessage("swagger.tag.auth.name", null, Locale.forLanguageTag(tag)))
                .as("fallback de %s", tag)
                .isEqualTo(english)
                .isNotEqualTo(portuguese);
        }
    }

    @Test
    void unknownLocaleStillResolvesMessagesThroughTheChain() {
        MessageSource source = config.messageSource();

        // zh-TW nao tem arquivo proprio: cai no bundle de idioma (zh).
        assertThat(source.getMessage("book.not-found", null, Locale.forLanguageTag("zh-TW")))
            .isEqualTo("未找到图书。");
        // Tag sem bundle nenhum percorre a cadeia e para no primeiro degrau: ingles.
        assertThat(source.getMessage("book.not-found", null, Locale.forLanguageTag("xx-YY")))
            .isEqualTo("Book not found.");
        // Ingles generico nao tem arquivo _en: a cadeia entrega o _en_US.
        assertThat(source.getMessage("book.not-found", null, Locale.forLanguageTag("en")))
            .isEqualTo("Book not found.");
    }
}
