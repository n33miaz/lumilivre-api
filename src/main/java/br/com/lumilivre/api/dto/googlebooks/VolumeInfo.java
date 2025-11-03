package br.com.lumilivre.api.dto.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VolumeInfo(
        String title,
        List<String> authors,
        String publisher,
        String publishedDate,
        String description,
        Integer pageCount,
        List<String> categories,
        ImageLinks imageLinks) {
}