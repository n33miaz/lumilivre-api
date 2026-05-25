package br.com.lumilivre.api.service.infra.postalcode;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Provider universal usando Zippopotam.us (gratuito, ~60 países, sem auth).
 * Cobre os ZIPs/postcodes que ViaCEP não atende — endpoint:
 * {@code https://api.zippopotam.us/{country}/{postcode}}.
 *
 * Declara {@code "*"} em {@link #supportedCountryCodes()} para que o
 * {@link PostalCodeRouter} o use como fallback universal.
 */
@Component
@ConditionalOnProperty(name = "lumilivre.postal-code.zippopotam.enabled", havingValue = "true", matchIfMissing = true)
public class UniversalPostalCodeProvider implements PostalCodeProvider {

    private static final String ZIPPOPOTAM_URL = "https://api.zippopotam.us/%s/%s";
    private static final Logger log = LoggerFactory.getLogger(UniversalPostalCodeProvider.class);

    private final RestTemplate restTemplate;

    public UniversalPostalCodeProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String name() {
        return "zippopotam";
    }

    @Override
    public Set<String> supportedCountryCodes() {
        return Set.of("*");
    }

    @Override
    @CircuitBreaker(name = "postalCode", fallbackMethod = "lookupFallback")
    @Retry(name = "postalCode")
    public Optional<PostalAddress> lookup(String code, String countryCode) {
        if (code == null || countryCode == null) return Optional.empty();
        String iso = countryCode.toLowerCase(Locale.ROOT);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(String.format(ZIPPOPOTAM_URL, iso, code), Map.class);
            if (response == null) {
                return Optional.empty();
            }
            return Optional.of(toAddress(response, countryCode));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    private Optional<PostalAddress> lookupFallback(String code, String countryCode, Throwable t) {
        log.warn("Zippopotam indisponível para {}-{}: {}", countryCode, code, t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private PostalAddress toAddress(Map<String, Object> response, String countryCode) {
        String postalCode = (String) response.get("post code");
        Object places = response.get("places");
        String city = null;
        String regionCode = null;
        String region = null;

        if (places instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            city = stringValue(first, "place name");
            regionCode = stringValue(first, "state abbreviation");
            region = stringValue(first, "state");
        }

        return new PostalAddress(
                postalCode,
                countryCode.toUpperCase(Locale.ROOT),
                null,                     // street não vem do Zippopotam
                null,
                null,
                city,
                regionCode,
                region
        );
    }

    private String stringValue(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : null;
    }
}
