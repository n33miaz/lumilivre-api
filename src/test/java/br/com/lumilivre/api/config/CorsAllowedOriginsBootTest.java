package br.com.lumilivre.api.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationContextException;

/**
 * Guards the boot-time contract for {@code app.cors.allowed-origins}.
 *
 * <p>Delegates to {@link SecurityConfig#validateCorsOrigins()} — the same hook
 * Spring invokes during bean initialization. Keeping the test at unit level
 * avoids spinning up a full ApplicationContext while still covering every
 * accept/reject branch.</p>
 */
class CorsAllowedOriginsBootTest {

    @ParameterizedTest(name = "rejects origin list \"{0}\" with reason [{1}]")
    @CsvSource(value = {
            "''                                       , at least one origin",
            "'   '                                    , blank entry",
            "'*'                                      , cannot be '*'",
            "'not-a-url'                              , absolute http(s)",
            "' http://localhost:5173'                 , whitespace",
            "'http://localhost:5173, https://ok.com'  , whitespace",
            "'http://localhost:5173/'                 , must not end with '/'",
            "'http://localhost:5173/path'             , must not include path",
            "'https://app.lumilivre.com.br?x=1'       , must not include path",
            "'http://localhost:5173,'                 , blank entry",
            "'http://localhost:5173, ,https://ok.com' , blank entry"
    })
    @DisplayName("validateCorsOrigins rejects malformed entries")
    void rejectsInvalidOriginLists(String origins, String expectedFragment) throws Exception {
        SecurityConfig cfg = newConfigWithOrigins(origins);

        assertThatThrownBy(cfg::validateCorsOrigins)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessageContaining(expectedFragment);
    }

    @Test
    @DisplayName("validateCorsOrigins rejects absent property")
    void rejectsNullArray() throws Exception {
        SecurityConfig cfg = newConfigWithOrigins(null);

        assertThatThrownBy(cfg::validateCorsOrigins)
                .isInstanceOf(ApplicationContextException.class)
                .hasMessageContaining("at least one origin");
    }

    @ParameterizedTest(name = "accepts origin list \"{0}\"")
    @ValueSource(strings = {
            "http://localhost:5173",
            "http://localhost:5173,http://localhost:8080",
            "https://app.lumilivre.com.br,https://admin.lumilivre.com.br"
    })
    @DisplayName("validateCorsOrigins accepts well-formed lists")
    void acceptsValidOriginLists(String origins) throws Exception {
        SecurityConfig cfg = newConfigWithOrigins(origins);

        assertThatCode(cfg::validateCorsOrigins).doesNotThrowAnyException();
    }

    private static SecurityConfig newConfigWithOrigins(String origins) throws Exception {
        SecurityConfig cfg = new SecurityConfig(null, null, null, null, null, null);
        Field f = SecurityConfig.class.getDeclaredField("allowedOrigins");
        f.setAccessible(true);
        String[] value;
        if (origins == null) {
            value = null;
        } else if (origins.isEmpty()) {
            value = new String[0];
        } else {
            value = origins.split(",", -1);
        }
        f.set(cfg, value);
        return cfg;
    }
}
