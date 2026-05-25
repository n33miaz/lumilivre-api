package br.com.lumilivre.api.service.infra.postalcode;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import br.com.lumilivre.api.config.CacheNames;

/**
 * Roteador de {@link PostalCodeProvider}: escolhe o provider apropriado para o
 * país solicitado. Preferência:
 * <ol>
 *   <li>provider que declara o país explicitamente em
 *       {@link PostalCodeProvider#supportedCountryCodes()};</li>
 *   <li>provider universal (declara {@code "*"}).</li>
 * </ol>
 *
 * Cacheia o resultado por chave {@code (countryCode, postalCode)} no namespace
 * {@code postal-codes} (TTL configurado em {@link CacheNames}).
 */
@Component
public class PostalCodeRouter {

    private static final Logger log = LoggerFactory.getLogger(PostalCodeRouter.class);

    private final List<PostalCodeProvider> providers;

    public PostalCodeRouter(List<PostalCodeProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Cacheable(value = CacheNames.POSTAL_CODES, key = "#countryCode + ':' + #code", unless = "#result == null")
    public Optional<PostalAddress> lookup(String code, String countryCode) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String iso = (countryCode == null || countryCode.isBlank()) ? "BR" : countryCode.toUpperCase(Locale.ROOT);
        for (PostalCodeProvider provider : prioritized(iso)) {
            try {
                Optional<PostalAddress> result = provider.lookup(code, iso);
                if (result.isPresent()) {
                    log.debug("Postal code {}:{} resolvido por '{}'", iso, code, provider.name());
                    return result;
                }
            } catch (Exception e) {
                log.warn("Provider {} falhou: {}", provider.name(), e.getMessage());
            }
        }
        return Optional.empty();
    }

    private List<PostalCodeProvider> prioritized(String iso) {
        List<PostalCodeProvider> exact = providers.stream()
                .filter(p -> p.supportedCountryCodes().contains(iso))
                .toList();
        List<PostalCodeProvider> universal = providers.stream()
                .filter(p -> p.supportedCountryCodes().contains("*"))
                .toList();
        return java.util.stream.Stream.concat(exact.stream(), universal.stream()).distinct().toList();
    }
}
