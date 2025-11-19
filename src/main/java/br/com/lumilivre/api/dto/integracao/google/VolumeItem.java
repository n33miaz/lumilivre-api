package br.com.lumilivre.api.dto.integracao.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VolumeItem(VolumeInfo volumeInfo) {
}