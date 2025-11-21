package br.com.lumilivre.api.dto.integracao.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record ImageLinks(
        String smallThumbnail,
        String thumbnail,
        String medium,
        String large,
        String extraLarge) {
}