package br.com.lumilivre.api.service.infra.bookmetadata;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import br.com.lumilivre.api.dto.integration.google.GoogleBooksResponse;
import br.com.lumilivre.api.dto.integration.google.ImageLinks;
import br.com.lumilivre.api.dto.integration.google.VolumeInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * {@link BookMetadataProvider} para Google Books API. Mantém circuit breaker e
 * retry já configurados em {@code application.properties} (instance
 * {@code googleBooks}).
 */
@Component
@ConditionalOnProperty(name = "lumilivre.book-metadata.google-books.enabled", havingValue = "true", matchIfMissing = true)
public class GoogleBooksProvider implements BookMetadataProvider {

    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes";
    private static final Logger log = LoggerFactory.getLogger(GoogleBooksProvider.class);

    private final RestTemplate restTemplate;

    public GoogleBooksProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String name() {
        return "googleBooks";
    }

    @Override
    public Optional<BookMetadata> findByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        return query("isbn:" + isbn);
    }

    @Override
    public Optional<BookMetadata> findByTitleAndAuthor(String title, String author) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }
        StringBuilder q = new StringBuilder("intitle:").append(title);
        if (author != null && !author.isBlank()) {
            q.append("+inauthor:").append(author);
        }
        return query(q.toString());
    }

    @CircuitBreaker(name = "googleBooks", fallbackMethod = "queryFallback")
    @Retry(name = "googleBooks")
    Optional<BookMetadata> query(String q) {
        String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_BOOKS_API)
                .queryParam("q", q)
                .queryParam("maxResults", 1)
                .toUriString();
        GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return Optional.empty();
        }
        return toMetadata(response.items().get(0).volumeInfo());
    }

    @SuppressWarnings("unused")
    private Optional<BookMetadata> queryFallback(String q, Throwable t) {
        log.warn("Google Books indisponível para query '{}': {}", q, t.getMessage());
        return Optional.empty();
    }

    private Optional<BookMetadata> toMetadata(VolumeInfo info) {
        if (info == null) {
            return Optional.empty();
        }
        BookMetadata.Builder b = BookMetadata.builder(name())
                .title(info.title())
                .publisher(info.publisher())
                .synopsis(info.description())
                .pageCount(info.pageCount())
                .rating(info.averageRating())
                .categories(info.categories());

        if (info.authors() != null && !info.authors().isEmpty()) {
            b.author(String.join(", ", info.authors()));
        }
        parsePublicationDate(info.publishedDate()).ifPresent(b::publicationDate);
        imageUrl(info.imageLinks()).ifPresent(b::coverUrl);
        return Optional.of(b.build());
    }

    private Optional<LocalDate> parsePublicationDate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try { return Optional.of(LocalDate.parse(raw)); } catch (DateTimeParseException ignored) {}
        try { return Optional.of(YearMonth.parse(raw).atDay(1)); } catch (DateTimeParseException ignored) {}
        try { return Optional.of(Year.parse(raw).atDay(1)); } catch (DateTimeParseException ignored) {}
        return Optional.empty();
    }

    private Optional<String> imageUrl(ImageLinks links) {
        if (links == null) return Optional.empty();
        String url = Optional.ofNullable(links.extraLarge())
                .or(() -> Optional.ofNullable(links.large()))
                .or(() -> Optional.ofNullable(links.medium()))
                .or(() -> Optional.ofNullable(links.thumbnail()))
                .or(() -> Optional.ofNullable(links.smallThumbnail()))
                .orElse(null);
        if (url != null && url.startsWith("http://")) {
            url = url.replaceFirst("http://", "https://");
        }
        return Optional.ofNullable(url);
    }
}
