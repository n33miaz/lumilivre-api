package br.com.lumilivre.api.dto.integracao.google;

public record GoogleBookSimpleResponse(
        String title,
        String author,
        String publisher,
        String publishedDate,
        String description,
        String isbn,
        String thumbnail) {
}
