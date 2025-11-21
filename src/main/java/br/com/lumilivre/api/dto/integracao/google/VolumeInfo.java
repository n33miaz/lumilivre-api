package br.com.lumilivre.api.dto.integracao.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
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