package br.com.lumilivre.api.service.infra.bookmetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class OpenLibraryProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void findByIsbnReturnsEmptyForBlankIsbn() {
        assertThat(provider().findByIsbn(" ")).isEmpty();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void findByIsbnParsesFullPayload() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response(book()
                .title("Clean Code")
                .publisher("Prentice Hall")
                .authors("Robert C. Martin", "Uncle Bob")
                .subjects("Software engineering", "Programming")
                .pages(464)
                .publishDate("2024-09-15")
                .cover("https://cdn.test/large.jpg", "https://cdn.test/medium.jpg")
                .build()));

        BookMetadata metadata = provider().findByIsbn("9780132350884").orElseThrow();

        assertThat(metadata.providerName()).isEqualTo("openLibrary");
        assertThat(metadata.isbn()).isEqualTo("9780132350884");
        assertThat(metadata.title()).isEqualTo("Clean Code");
        assertThat(metadata.publisher()).isEqualTo("Prentice Hall");
        assertThat(metadata.author()).isEqualTo("Robert C. Martin, Uncle Bob");
        assertThat(metadata.categories()).containsExactly("Software engineering", "Programming");
        assertThat(metadata.pageCount()).isEqualTo(464);
        assertThat(metadata.publicationDate()).isEqualTo(LocalDate.of(2024, 9, 15));
        assertThat(metadata.coverUrl()).isEqualTo("https://cdn.test/large.jpg");
    }

    @Test
    void findByIsbnNormalizesInputAndUsesFallbackCover() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response(book()
                .title("Refactoring")
                .publishDate("2024-09")
                .build()));

        BookMetadata metadata = provider().findByIsbn("978-0-13-235088-4").orElseThrow();

        assertThat(metadata.isbn()).isEqualTo("9780132350884");
        assertThat(metadata.publicationDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        assertThat(metadata.coverUrl()).isEqualTo(
                "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg");
    }

    @Test
    void findByIsbnParsesYearOnlyAndTextualYearDates() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(response(book().title("Year").publishDate("2024").build()))
                .thenReturn(response(book().title("Text Year").publishDate("September 2025").build()));

        assertThat(provider().findByIsbn("111").orElseThrow().publicationDate())
                .isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(provider().findByIsbn("222").orElseThrow().publicationDate())
                .isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    void findByIsbnReturnsEmptyForEmptyResponseOrClientFailure() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of())
                .thenThrow(new RuntimeException("network down"));

        assertThat(provider().findByIsbn("111")).isEmpty();
        assertThat(provider().findByIsbn("222")).isEmpty();
    }

    private OpenLibraryProvider provider() {
        return new OpenLibraryProvider(restTemplate);
    }

    private static Map<String, Map<String, Object>> response(Map<String, Object> book) {
        Map<String, Map<String, Object>> response = new LinkedHashMap<>();
        response.put("ISBN:9780132350884", book);
        return response;
    }

    private static BookPayloadBuilder book() {
        return new BookPayloadBuilder();
    }

    private static final class BookPayloadBuilder {
        private final Map<String, Object> values = new LinkedHashMap<>();

        BookPayloadBuilder title(String title) {
            values.put("title", title);
            return this;
        }

        BookPayloadBuilder publisher(String name) {
            values.put("publishers", List.of(Map.of("name", name)));
            return this;
        }

        BookPayloadBuilder authors(String... names) {
            values.put("authors", List.of(names).stream()
                    .map(name -> Map.of("name", name))
                    .toList());
            return this;
        }

        BookPayloadBuilder subjects(String... names) {
            values.put("subjects", List.of(names).stream()
                    .map(name -> Map.of("name", name))
                    .toList());
            return this;
        }

        BookPayloadBuilder pages(int pages) {
            values.put("number_of_pages", pages);
            return this;
        }

        BookPayloadBuilder publishDate(String date) {
            values.put("publish_date", date);
            return this;
        }

        BookPayloadBuilder cover(String large, String medium) {
            values.put("cover", Map.of("large", large, "medium", medium));
            return this;
        }

        Map<String, Object> build() {
            return values;
        }
    }
}
