package br.com.lumilivre.api.service.infra.postalcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class UniversalPostalCodeProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void nameIsZippopotam() {
        assertThat(provider().name()).isEqualTo("zippopotam");
    }

    @Test
    void declaresWildcardCountrySupport() {
        assertThat(provider().supportedCountryCodes()).containsExactly("*");
    }

    @Test
    void lookupReturnsEmptyWhenInputsAreNull() {
        assertThat(provider().lookup(null, "US")).isEmpty();
        assertThat(provider().lookup("90210", null)).isEmpty();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void lookupMapsZippopotamPayloadIntoPostalAddress() {
        Map<String, Object> place = Map.of(
                "place name", "Beverly Hills",
                "state", "California",
                "state abbreviation", "CA");
        Map<String, Object> response = Map.of(
                "post code", "90210",
                "country", "United States",
                "places", List.of(place));
        when(restTemplate.getForObject(eq("https://api.zippopotam.us/us/90210"), eq(Map.class)))
                .thenReturn(response);

        Optional<PostalAddress> result = provider().lookup("90210", "US");

        assertThat(result).isPresent();
        PostalAddress address = result.orElseThrow();
        assertThat(address.postalCode()).isEqualTo("90210");
        assertThat(address.countryCode()).isEqualTo("US");
        assertThat(address.city()).isEqualTo("Beverly Hills");
        assertThat(address.region()).isEqualTo("California");
        assertThat(address.regionCode()).isEqualTo("CA");
        assertThat(address.street()).isNull();
    }

    @Test
    void lookupLowercasesCountryCodeForRemoteCallButPreservesIsoOnResponse() {
        Map<String, Object> response = Map.of(
                "post code", "1000",
                "places", List.of(Map.of("place name", "Lisboa")));
        when(restTemplate.getForObject(eq("https://api.zippopotam.us/pt/1000"), eq(Map.class)))
                .thenReturn(response);

        PostalAddress address = provider().lookup("1000", "pt").orElseThrow();

        assertThat(address.countryCode()).isEqualTo("PT");
        assertThat(address.city()).isEqualTo("Lisboa");
    }

    @Test
    void lookupReturnsEmptyForNullResponseOrNotFound() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(null)
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        assertThat(provider().lookup("00000", "US")).isEmpty();
        assertThat(provider().lookup("11111", "US")).isEmpty();
    }

    private UniversalPostalCodeProvider provider() {
        return new UniversalPostalCodeProvider(restTemplate);
    }
}
