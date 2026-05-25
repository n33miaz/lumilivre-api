package br.com.lumilivre.api.service.infra.bookmetadata;

import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.dto.integration.brasilapi.BrasilApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * {@link BookMetadataProvider} para BrasilAPI (ISBN). Cobre catálogo PT-BR
 * — usar em conjunto com Google Books como fallback para obras nacionais.
 */
@Component
@ConditionalOnProperty(name = "lumilivre.book-metadata.brasil-api.enabled", havingValue = "true", matchIfMissing = true)
public class BrasilApiProvider implements BookMetadataProvider {

    private static final String BRASIL_API_URL = "https://brasilapi.com.br/api/isbn/v1/";
    private static final Logger log = LoggerFactory.getLogger(BrasilApiProvider.class);

    private final RestTemplate restTemplate;

    public BrasilApiProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String name() {
        return "brasilApi";
    }

    @Override
    @CircuitBreaker(name = "brasilApi", fallbackMethod = "findByIsbnFallback")
    @Retry(name = "brasilApi")
    public Optional<BookMetadata> findByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        String isbnLimpo = isbn.replaceAll("[^0-9]", "");
        try {
            BrasilApiResponse response = restTemplate.getForObject(BRASIL_API_URL + isbnLimpo, BrasilApiResponse.class);
            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(toMetadata(response));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    private Optional<BookMetadata> findByIsbnFallback(String isbn, Throwable t) {
        log.warn("BrasilAPI indisponível para ISBN '{}': {}", isbn, t.getMessage());
        return Optional.empty();
    }

    private BookMetadata toMetadata(BrasilApiResponse r) {
        BookMetadata.Builder b = BookMetadata.builder(name())
                .isbn(r.isbn())
                .title(r.title())
                .publisher(r.publisher())
                .synopsis(r.synopsis())
                .pageCount(r.pageCount())
                .coverUrl(r.coverUrl());

        if (r.authors() != null && !r.authors().isEmpty()) {
            b.author(String.join(", ", r.authors()));
        }
        if (r.year() != null) {
            b.publicationDate(LocalDate.of(r.year(), 1, 1));
        }
        return b.build();
    }
}
