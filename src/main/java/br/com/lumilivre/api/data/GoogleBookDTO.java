package br.com.lumilivre.api.data;

import java.util.List;

public record GoogleBookDTO(
        String title,
        String author, // só o primeiro autor
        String publisher,
        String publishedDate,
        String description,
        String isbn,
        String thumbnail
) {}
