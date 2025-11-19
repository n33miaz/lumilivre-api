package br.com.lumilivre.api.dto;

public record GoogleBookDTO(
                String title,
                String author,
                String publisher,
                String publishedDate,
                String description,
                String isbn,
                String thumbnail) {
}
