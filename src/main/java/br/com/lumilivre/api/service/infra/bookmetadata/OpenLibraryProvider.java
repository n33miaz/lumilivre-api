package br.com.lumilivre.api.service.infra.bookmetadata;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * {@link BookMetadataProvider} para OpenLibrary (api.openlibrary.org). Cobre
 * acervos globais — usado como fallback quando Google Books/BrasilAPI não
 * conhecem a obra. Endpoint não exige autenticação e retorna JSON simples.
 *
 * Resiliência reutiliza a instance {@code openLibrary} configurada em
 * {@code application.properties}.
 */
@Component
@ConditionalOnProperty(name = "lumilivre.book-metadata.open-library.enabled", havingValue = "true", matchIfMissing = true)
public class OpenLibraryProvider implements BookMetadataProvider {

    private static final String OPEN_LIBRARY_API = "https://openlibrary.org/api/books";
    private static final String COVER_URL_TEMPLATE = "https://covers.openlibrary.org/b/isbn/%s-L.jpg";
    private static final Logger log = LoggerFactory.getLogger(OpenLibraryProvider.class);

    private final RestTemplate restTemplate;

    public OpenLibraryProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String name() {
        return "openLibrary";
    }

    @Override
    @CircuitBreaker(name = "openLibrary", fallbackMethod = "findByIsbnFallback")
    @Retry(name = "openLibrary")
    public Optional<BookMetadata> findByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        String isbnLimpo = isbn.replaceAll("[^0-9Xx]", "");
        String url = OPEN_LIBRARY_API
                + "?bibkeys=ISBN:" + isbnLimpo
                + "&format=json&jscmd=data";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Object> book = response.values().iterator().next();
            return Optional.of(toMetadata(book, isbnLimpo));
        } catch (Exception e) {
            log.debug("OpenLibrary não retornou dados para ISBN '{}': {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    private Optional<BookMetadata> findByIsbnFallback(String isbn, Throwable t) {
        log.warn("OpenLibrary indisponível para ISBN '{}': {}", isbn, t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private BookMetadata toMetadata(Map<String, Object> book, String isbn) {
        BookMetadata.Builder b = BookMetadata.builder(name())
                .isbn(isbn)
                .title((String) book.get("title"));

        Object publishers = book.get("publishers");
        if (publishers instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map && map.get("name") instanceof String s) {
                b.publisher(s);
            }
        }

        Object authors = book.get("authors");
        if (authors instanceof List<?> list && !list.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> map && map.get("name") instanceof String s) {
                    names.add(s);
                }
            }
            if (!names.isEmpty()) {
                b.author(String.join(", ", names));
            }
        }

        Object subjects = book.get("subjects");
        if (subjects instanceof List<?> list && !list.isEmpty()) {
            List<String> subjectNames = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> map && map.get("name") instanceof String s) {
                    subjectNames.add(s);
                }
            }
            if (!subjectNames.isEmpty()) {
                b.categories(subjectNames);
            }
        }

        Object pages = book.get("number_of_pages");
        if (pages instanceof Number n) {
            b.pageCount(n.intValue());
        }

        Object publishDate = book.get("publish_date");
        if (publishDate instanceof String s) {
            parsePublicationDate(s).ifPresent(b::publicationDate);
        }

        Object cover = book.get("cover");
        if (cover instanceof Map<?, ?> map) {
            Object large = map.get("large");
            if (large instanceof String s) {
                b.coverUrl(s);
            } else if (map.get("medium") instanceof String m) {
                b.coverUrl(m);
            }
        }
        // Fallback de capa via padrão estável OpenLibrary
        if (b.build().coverUrl() == null) {
            b.coverUrl(String.format(COVER_URL_TEMPLATE, isbn));
        }

        return b.build();
    }

    private Optional<LocalDate> parsePublicationDate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try { return Optional.of(LocalDate.parse(raw)); } catch (DateTimeParseException ignored) {}
        try { return Optional.of(YearMonth.parse(raw).atDay(1)); } catch (DateTimeParseException ignored) {}
        try { return Optional.of(Year.parse(raw).atDay(1)); } catch (DateTimeParseException ignored) {}
        try {
            // formatos "2024" ou "Sep 2024" / "September 2024"
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.length() >= 4) {
                return Optional.of(Year.of(Integer.parseInt(digits.substring(digits.length() - 4))).atDay(1));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }
}
