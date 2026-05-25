package br.com.lumilivre.api.service.infra.postalcode;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.dto.common.AddressLookupResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Provider para CEPs brasileiros (8 dígitos) via ViaCEP. Resiliência configurada
 * na instance {@code postalCode}.
 */
@Component
@ConditionalOnProperty(name = "lumilivre.postal-code.viacep.enabled", havingValue = "true", matchIfMissing = true)
public class BrazilianPostalCodeProvider implements PostalCodeProvider {

    private static final String VIACEP_URL = "https://viacep.com.br/ws/%s/json/";
    private static final Logger log = LoggerFactory.getLogger(BrazilianPostalCodeProvider.class);

    private final RestTemplate restTemplate;

    public BrazilianPostalCodeProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String name() {
        return "viaCep";
    }

    @Override
    public Set<String> supportedCountryCodes() {
        return Set.of("BR");
    }

    @Override
    @CircuitBreaker(name = "postalCode", fallbackMethod = "lookupFallback")
    @Retry(name = "postalCode")
    public Optional<PostalAddress> lookup(String code, String countryCode) {
        if (code == null) return Optional.empty();
        String digits = code.replaceAll("\\D", "");
        if (digits.length() != 8) {
            return Optional.empty();
        }
        AddressLookupResponse raw = restTemplate.getForObject(String.format(VIACEP_URL, digits), AddressLookupResponse.class);
        if (raw == null || raw.getLogradouro() == null) {
            return Optional.empty();
        }
        return Optional.of(new PostalAddress(
                digits,
                "BR",
                raw.getLogradouro(),
                raw.getComplemento(),
                raw.getBairro(),
                raw.getLocalidade(),
                raw.getUf(),
                raw.getUf()
        ));
    }

    @SuppressWarnings("unused")
    private Optional<PostalAddress> lookupFallback(String code, String countryCode, Throwable t) {
        log.warn("ViaCEP indisponível para CEP '{}': {}", code, t.getMessage());
        return Optional.empty();
    }
}
