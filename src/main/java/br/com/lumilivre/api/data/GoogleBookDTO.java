package br.com.lumilivre.api.data;

public record GoogleBookDTO(
        String title,
        String author,
        String publisher,
        String publishedDate,
        String description,
        String isbn,
        String thumbnail) {
}
