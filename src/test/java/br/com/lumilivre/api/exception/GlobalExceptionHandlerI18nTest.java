package br.com.lumilivre.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.common.ErrorResponse;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

class GlobalExceptionHandlerI18nTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        I18nConfig config = new I18nConfig();
        MessageSource source = config.messageSource();
        handler = new GlobalExceptionHandler(new MessageResolver(source));
    }

    @ParameterizedTest(name = "locale={0} => message={1}")
    @CsvSource({
        "pt-BR, Leitor não encontrado.",
        "en-US, Reader not found."
    })
    void resourceNotFoundResolvesPerLocale(String tag, String expectedMessage) {
        var ex = ResourceNotFoundException.ofKey("reader.not-found");
        Locale locale = Locale.forLanguageTag(tag);
        WebRequest req = webRequest("GET", "/readers");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, locale, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getFirst("Content-Language")).isEqualTo(tag);
        assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
    }

    @ParameterizedTest(name = "locale={0} => message={1}")
    @CsvSource({
        "pt-BR, Limite de empréstimos ativos atingido.",
        "en-US, Maximum active loans limit reached."
    })
    void businessRuleResolvesPerLocale(String tag, String expectedMessage) {
        var ex = BusinessRuleException.ofKey("loan.policy-violation.max-active-loans-reached");
        Locale locale = Locale.forLanguageTag(tag);
        WebRequest req = webRequest("GET", "/loans");

        ResponseEntity<ErrorResponse> response = handler.handleBusinessRule(ex, locale, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getFirst("Content-Language")).isEqualTo(tag);
        assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
    }

    @ParameterizedTest(name = "locale={0} => error={1}")
    @CsvSource({
        "pt-BR, Erro de Validação",
        "en-US, Validation Error"
    })
    void commonErrorTitlesResolvePerLocale(String tag, String expectedError) {
        Locale locale = Locale.forLanguageTag(tag);
        WebRequest req = webRequest("POST", "/readers");

        ErrorResponse body = handler.handleResourceNotFound(
            new ResourceNotFoundException("any"), locale, req).getBody();

        assertThat(handler.handleAccessDenied(
            new org.springframework.security.access.AccessDeniedException("x"), locale, req)
            .getBody().getError()).isNotBlank();

        ResponseEntity<ErrorResponse> validationResponse = handler.handleAuthentication(
            new org.springframework.security.authentication.BadCredentialsException("x"), locale, req);
        assertThat(validationResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(validationResponse.getHeaders().getFirst("Content-Language")).isEqualTo(tag);
    }

    @Test
    void bookNotFoundResolvesEnUS() {
        var ex = ResourceNotFoundException.ofKey("book.not-found");
        Locale locale = Locale.forLanguageTag("en-US");
        WebRequest req = webRequest("GET", "/books");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, locale, req);

        assertThat(response.getBody().getMessage()).isEqualTo("Book not found.");
    }

    @Test
    void loanNotFoundResolvesPtBR() {
        var ex = ResourceNotFoundException.ofKey("loan.not-found");
        Locale locale = Locale.forLanguageTag("pt-BR");
        WebRequest req = webRequest("GET", "/loans");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, locale, req);

        assertThat(response.getBody().getMessage()).isEqualTo("Empréstimo não encontrado.");
    }

    @Test
    void literalMessageFallsThrough() {
        var ex = new ResourceNotFoundException("Mensagem literal sem chave");
        Locale locale = Locale.forLanguageTag("en-US");
        WebRequest req = webRequest("GET", "/any");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, locale, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Mensagem literal sem chave");
    }

    private static WebRequest webRequest(String method, String path) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        return new ServletWebRequest(req);
    }
}
