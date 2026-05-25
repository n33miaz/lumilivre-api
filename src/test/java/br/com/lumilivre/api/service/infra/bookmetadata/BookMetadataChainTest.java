package br.com.lumilivre.api.service.infra.bookmetadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BookMetadataChainTest {

    @Test
    void returnsFirstProviderResultWhenComplete() {
        BookMetadataProvider google = stub("googleBooks", Optional.of(complete("googleBooks")));
        BookMetadataProvider brasil = stub("brasilApi", Optional.of(other("brasilApi")));
        BookMetadataChain chain = new BookMetadataChain(List.of(google, brasil), "googleBooks,brasilApi");

        Optional<BookMetadata> result = chain.findByIsbn("9788535914849");

        assertThat(result).isPresent();
        BookMetadata metadata = result.orElseThrow();
        assertThat(metadata.providerName()).isEqualTo("googleBooks");
        assertThat(metadata.title()).isEqualTo("Dom Casmurro");
    }

    @Test
    void mergesMissingFieldsFromSecondaryProvider() {
        BookMetadata partial = BookMetadata.builder("googleBooks")
                .title("Dom Casmurro")
                .author("Machado de Assis")
                .build();
        BookMetadata complement = BookMetadata.builder("brasilApi")
                .publisher("Companhia das Letras")
                .synopsis("Romance clássico brasileiro.")
                .coverUrl("https://cover.example/dom.jpg")
                .publicationDate(LocalDate.of(1899, 1, 1))
                .build();

        BookMetadataChain chain = new BookMetadataChain(
                List.of(stub("googleBooks", Optional.of(partial)),
                        stub("brasilApi", Optional.of(complement))),
                "googleBooks,brasilApi");

        BookMetadata merged = chain.findByIsbn("9788535914849").orElseThrow();

        assertThat(merged.title()).isEqualTo("Dom Casmurro");
        assertThat(merged.author()).isEqualTo("Machado de Assis");
        assertThat(merged.publisher()).isEqualTo("Companhia das Letras");
        assertThat(merged.synopsis()).contains("Romance clássico");
        assertThat(merged.coverUrl()).isEqualTo("https://cover.example/dom.jpg");
        assertThat(merged.publicationDate()).isEqualTo(LocalDate.of(1899, 1, 1));
        assertThat(merged.providerName()).isEqualTo("googleBooks+brasilApi");
    }

    @Test
    void skipsProvidersThatThrow() {
        BookMetadataProvider failing = new BookMetadataProvider() {
            @Override public String name() { return "googleBooks"; }
            @Override public Optional<BookMetadata> findByIsbn(String isbn) {
                throw new RuntimeException("circuit open");
            }
        };
        BookMetadataProvider working = stub("brasilApi", Optional.of(complete("brasilApi")));

        BookMetadataChain chain = new BookMetadataChain(List.of(failing, working), "googleBooks,brasilApi");

        BookMetadata result = chain.findByIsbn("9788535914849").orElseThrow();
        assertThat(result.providerName()).isEqualTo("brasilApi");
    }

    @Test
    void respectsConfiguredOrder() {
        BookMetadataProvider google = stub("googleBooks", Optional.of(complete("googleBooks")));
        BookMetadataProvider brasil = stub("brasilApi", Optional.of(other("brasilApi")));

        // Inverte a ordem na config — brasilApi vem primeiro
        BookMetadataChain chain = new BookMetadataChain(List.of(google, brasil), "brasilApi,googleBooks");
        BookMetadata result = chain.findByIsbn("9788535914849").orElseThrow();

        assertThat(result.providerName()).isEqualTo("brasilApi");
    }

    @Test
    void ignoresUnknownProviderNameInConfig() {
        BookMetadataProvider google = stub("googleBooks", Optional.of(complete("googleBooks")));
        BookMetadataChain chain = new BookMetadataChain(List.of(google), "unknownProvider,googleBooks");

        assertThat(chain.findByIsbn("9788535914849")).isPresent();
    }

    @Test
    void returnsEmptyWhenAllProvidersAreEmpty() {
        BookMetadataChain chain = new BookMetadataChain(
                List.of(stub("googleBooks", Optional.empty()),
                        stub("brasilApi", Optional.empty())),
                "googleBooks,brasilApi");
        assertThat(chain.findByIsbn("0000000000")).isEmpty();
    }

    @Test
    void titleAndAuthorFallbackUsesProviderQuery() {
        BookMetadata metadata = BookMetadata.builder("googleBooks")
                .title("Dom Casmurro")
                .author("Machado de Assis")
                .publisher("Default")
                .synopsis("x")
                .coverUrl("https://x.jpg")
                .publicationDate(LocalDate.of(1899, 1, 1))
                .build();

        BookMetadataProvider google = new BookMetadataProvider() {
            @Override public String name() { return "googleBooks"; }
            @Override public Optional<BookMetadata> findByIsbn(String isbn) { return Optional.empty(); }
            @Override public Optional<BookMetadata> findByTitleAndAuthor(String t, String a) {
                return Optional.of(metadata);
            }
        };
        BookMetadataChain chain = new BookMetadataChain(List.of(google), "googleBooks");

        Optional<BookMetadata> result = chain.findByTitleAndAuthor("Dom", "Machado");
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().author()).contains("Machado");
    }

    // ----- helpers ----------------------------------------------------------

    private static BookMetadata complete(String provider) {
        return BookMetadata.builder(provider)
                .title("Dom Casmurro")
                .author("Machado de Assis")
                .publisher("Companhia das Letras")
                .synopsis("Romance clássico brasileiro.")
                .coverUrl("https://cover.example/dom.jpg")
                .publicationDate(LocalDate.of(1899, 1, 1))
                .pageCount(256)
                .rating(4.6)
                .categories(Set.of("Romance").stream().toList())
                .build();
    }

    private static BookMetadata other(String provider) {
        return BookMetadata.builder(provider)
                .title("Different title")
                .author("Different author")
                .publisher("Different")
                .synopsis("Different synopsis")
                .coverUrl("https://different.example/x.jpg")
                .publicationDate(LocalDate.of(2000, 1, 1))
                .pageCount(100)
                .build();
    }

    private static BookMetadataProvider stub(String name, Optional<BookMetadata> response) {
        return new BookMetadataProvider() {
            @Override public String name() { return name; }
            @Override public Optional<BookMetadata> findByIsbn(String isbn) { return response; }
        };
    }
}
