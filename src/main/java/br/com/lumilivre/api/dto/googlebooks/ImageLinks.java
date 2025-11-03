package br.com.lumilivre.api.dto.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageLinks(
        String smallThumbnail,
        String thumbnail,
        String medium,
        String large,
        String extraLarge) {
}