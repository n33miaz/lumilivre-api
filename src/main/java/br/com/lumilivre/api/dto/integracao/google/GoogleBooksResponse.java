package br.com.lumilivre.api.dto.integracao.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record GoogleBooksResponse(List<VolumeItem> items) {
}