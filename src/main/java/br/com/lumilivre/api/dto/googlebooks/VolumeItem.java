package br.com.lumilivre.api.dto.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VolumeItem(VolumeInfo volumeInfo) {
}