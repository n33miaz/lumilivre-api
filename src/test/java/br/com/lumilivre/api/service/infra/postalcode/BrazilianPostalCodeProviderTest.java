package br.com.lumilivre.api.service.infra.postalcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.dto.common.AddressLookupResponse;

@ExtendWith(MockitoExtension.class)
class BrazilianPostalCodeProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void nameIsViaCep() {
        assertThat(provider().name()).isEqualTo("viaCep");
    }

    @Test
    void supportsOnlyBr() {
        assertThat(provider().supportedCountryCodes()).containsExactly("BR");
    }

    @Test
    void lookupReturnsEmptyForNullOrInvalidCep() {
        assertThat(provider().lookup(null, "BR")).isEmpty();
        assertThat(provider().lookup("1234", "BR")).isEmpty();
        assertThat(provider().lookup("123456789", "BR")).isEmpty();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void lookupStripsNonDigitsBeforeQueryingViaCep() {
        AddressLookupResponse raw = new AddressLookupResponse(
                "01001-000", "Praca da Se", "lado par", "Se", "Sao Paulo", "SP");
        when(restTemplate.getForObject(eq("https://viacep.com.br/ws/01001000/json/"),
                eq(AddressLookupResponse.class))).thenReturn(raw);

        Optional<PostalAddress> result = provider().lookup("01001-000", "BR");

        assertThat(result).isPresent();
        PostalAddress address = result.orElseThrow();
        assertThat(address.postalCode()).isEqualTo("01001000");
        assertThat(address.countryCode()).isEqualTo("BR");
        assertThat(address.street()).isEqualTo("Praca da Se");
        assertThat(address.district()).isEqualTo("Se");
        assertThat(address.city()).isEqualTo("Sao Paulo");
        assertThat(address.region()).isEqualTo("SP");
        assertThat(address.regionCode()).isEqualTo("SP");
    }

    @Test
    void lookupReturnsEmptyWhenViaCepReturnsNullResponse() {
        when(restTemplate.getForObject(anyString(), eq(AddressLookupResponse.class))).thenReturn(null);

        assertThat(provider().lookup("01001000", "BR")).isEmpty();
    }

    @Test
    void lookupReturnsEmptyWhenLogradouroIsMissing() {
        AddressLookupResponse raw = new AddressLookupResponse(
                "00000000", null, null, null, null, null);
        when(restTemplate.getForObject(anyString(), eq(AddressLookupResponse.class))).thenReturn(raw);

        assertThat(provider().lookup("00000000", "BR")).isEmpty();
    }

    private BrazilianPostalCodeProvider provider() {
        return new BrazilianPostalCodeProvider(restTemplate);
    }
}
