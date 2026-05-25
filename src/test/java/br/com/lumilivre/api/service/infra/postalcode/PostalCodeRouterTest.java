package br.com.lumilivre.api.service.infra.postalcode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class PostalCodeRouterTest {

    @Test
    void prefersExactCountryProviderOverUniversal() {
        AtomicInteger viaCepCalls = new AtomicInteger();
        AtomicInteger zippoCalls = new AtomicInteger();

        PostalCodeProvider viaCep = stub("viaCep", Set.of("BR"), (code, country) -> {
            viaCepCalls.incrementAndGet();
            return Optional.of(address("01001000", "BR", "Praca da Se", "Sao Paulo", "SP"));
        });
        PostalCodeProvider zippo = stub("zippopotam", Set.of("*"), (code, country) -> {
            zippoCalls.incrementAndGet();
            return Optional.of(address("01001000", "BR", null, "Diferente", "XX"));
        });

        PostalCodeRouter router = new PostalCodeRouter(List.of(zippo, viaCep));
        Optional<PostalAddress> result = router.lookup("01001000", "BR");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().city()).isEqualTo("Sao Paulo");
        assertThat(viaCepCalls.get()).isEqualTo(1);
        assertThat(zippoCalls.get()).isZero();
    }

    @Test
    void fallsBackToUniversalWhenExactReturnsEmpty() {
        PostalCodeProvider viaCep = stub("viaCep", Set.of("BR"), (c, co) -> Optional.empty());
        PostalCodeProvider zippo = stub("zippopotam", Set.of("*"),
                (c, co) -> Optional.of(address("90210", "US", null, "Beverly Hills", "CA")));

        PostalCodeRouter router = new PostalCodeRouter(List.of(viaCep, zippo));
        Optional<PostalAddress> result = router.lookup("90210", "US");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().city()).isEqualTo("Beverly Hills");
        assertThat(result.orElseThrow().regionCode()).isEqualTo("CA");
    }

    @Test
    void returnsEmptyForBlankCode() {
        PostalCodeProvider any = stub("viaCep", Set.of("BR"), (c, co) -> {
            throw new AssertionError("should not be called for blank code");
        });
        PostalCodeRouter router = new PostalCodeRouter(List.of(any));
        assertThat(router.lookup("", "BR")).isEmpty();
        assertThat(router.lookup(null, "BR")).isEmpty();
    }

    @Test
    void defaultsCountryToBrWhenNullOrBlank() {
        PostalCodeProvider viaCep = stub("viaCep", Set.of("BR"),
                (c, co) -> Optional.of(address(c, co, "rua", "city", "SP")));
        PostalCodeRouter router = new PostalCodeRouter(List.of(viaCep));

        assertThat(router.lookup("01001000", null).orElseThrow().countryCode()).isEqualTo("BR");
        assertThat(router.lookup("01001000", "").orElseThrow().countryCode()).isEqualTo("BR");
    }

    @Test
    void survivesProviderException() {
        PostalCodeProvider failing = stub("viaCep", Set.of("BR"), (c, co) -> {
            throw new RuntimeException("network down");
        });
        PostalCodeProvider zippo = stub("zippopotam", Set.of("*"),
                (c, co) -> Optional.of(address("01001000", "BR", null, "fallback", null)));

        PostalCodeRouter router = new PostalCodeRouter(List.of(failing, zippo));
        Optional<PostalAddress> result = router.lookup("01001000", "BR");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().city()).isEqualTo("fallback");
    }

    private static PostalCodeProvider stub(String name, Set<String> countries,
                                           java.util.function.BiFunction<String, String, Optional<PostalAddress>> impl) {
        return new PostalCodeProvider() {
            @Override public String name() { return name; }
            @Override public Set<String> supportedCountryCodes() { return countries; }
            @Override public Optional<PostalAddress> lookup(String code, String countryCode) {
                return impl.apply(code, countryCode);
            }
        };
    }

    private static PostalAddress address(String code, String country, String street, String city, String region) {
        return new PostalAddress(code, country, street, null, null, city, region, region);
    }
}
