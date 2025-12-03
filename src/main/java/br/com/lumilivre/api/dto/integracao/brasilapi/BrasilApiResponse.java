package br.com.lumilivre.api.dto.integracao.brasilapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record BrasilApiResponse(
        String isbn,
        String title,
        String subtitle,
        List<String> authors,
        String publisher,
        String synopsis,
        @JsonProperty("page_count") Integer pageCount,
        Integer year,
        @JsonProperty("cover_url") String coverUrl) {
}