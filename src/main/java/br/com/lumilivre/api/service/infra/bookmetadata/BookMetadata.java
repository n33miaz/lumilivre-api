package br.com.lumilivre.api.service.infra.bookmetadata;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO interno comum a todos os {@link BookMetadataProvider}. Substitui DTOs
 * específicos por provider (GoogleBooksResponse, BrasilApiResponse) na fronteira
 * do domínio — o {@code BookService} jamais vê o formato do provider externo.
 *
 * Campos faltantes são {@code null}; a {@link BookMetadataChain} mescla
 * resultados de múltiplos providers.
 *
 * @param providerName identifica o provider de origem ({@code "googleBooks"},
 *                     {@code "brasilApi"}, {@code "openLibrary"}). Permite logging,
 *                     auditoria e debug de qualidade de dado.
 */
public record BookMetadata(
        String isbn,
        String title,
        String author,
        String publisher,
        String synopsis,
        LocalDate publicationDate,
        Integer pageCount,
        String coverUrl,
        Double rating,
        List<String> categories,
        String providerName
) {
    /** Convenience builder com nulos seguros — útil em providers que mapeiam parcialmente. */
    public static Builder builder(String providerName) {
        return new Builder(providerName);
    }

    public static final class Builder {
        private String isbn;
        private String title;
        private String author;
        private String publisher;
        private String synopsis;
        private LocalDate publicationDate;
        private Integer pageCount;
        private String coverUrl;
        private Double rating;
        private List<String> categories;
        private final String providerName;

        private Builder(String providerName) {
            this.providerName = providerName;
        }

        public Builder isbn(String v) { this.isbn = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder author(String v) { this.author = v; return this; }
        public Builder publisher(String v) { this.publisher = v; return this; }
        public Builder synopsis(String v) { this.synopsis = v; return this; }
        public Builder publicationDate(LocalDate v) { this.publicationDate = v; return this; }
        public Builder pageCount(Integer v) { this.pageCount = v; return this; }
        public Builder coverUrl(String v) { this.coverUrl = v; return this; }
        public Builder rating(Double v) { this.rating = v; return this; }
        public Builder categories(List<String> v) { this.categories = v; return this; }

        public BookMetadata build() {
            return new BookMetadata(isbn, title, author, publisher, synopsis,
                    publicationDate, pageCount, coverUrl, rating, categories, providerName);
        }
    }
}
