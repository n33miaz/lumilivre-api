package br.com.lumilivre.api.dto.integracao.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record VolumeItem(VolumeInfo volumeInfo) {
}