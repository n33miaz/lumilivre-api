package br.com.lumilivre.api.service.infra.postalcode;

/**
 * DTO neutro de retorno para qualquer {@link PostalCodeProvider}. Substitui
 * {@code AddressLookupResponse} (formato ViaCEP) na fronteira do domínio,
 * permitindo provedores internacionais ao lado do BR.
 *
 * @param countryCode ISO 3166-1 alpha-2 (ex.: "BR", "US", "PT").
 * @param regionCode  sigla de estado/província (ex.: "SP", "CA"). Pode ser nulo.
 */
public record PostalAddress(
        String postalCode,
        String countryCode,
        String street,
        String addressComplement,
        String district,
        String city,
        String regionCode,
        String region
) {
}
