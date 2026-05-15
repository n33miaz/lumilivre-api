package br.com.lumilivre.api.dto.metadata;

public record PostalCodeResponse(
        String postalCode,
        String street,
        String addressComplement,
        String district,
        String city,
        String stateCode) {}
