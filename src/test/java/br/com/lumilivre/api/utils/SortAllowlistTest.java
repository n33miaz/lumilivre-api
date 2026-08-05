package br.com.lumilivre.api.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.lumilivre.api.exception.custom.BusinessRuleException;

/**
 * SEC-15. A query nativa da listagem de exemplares interpola o {@code ORDER BY}
 * como texto, então nada aqui pode confiar no nome que vem do cliente.
 */
class SortAllowlistTest {

    private static final SortAllowlist ALLOWLIST = SortAllowlist.of(
            "title", "l.title",
            "copyCode", "e.copy_code");

    @ParameterizedTest(name = "sort={0} é recusado")
    @ValueSource(strings = {
            "id;DROP TABLE book--",
            "id; DELETE FROM app_user",
            "(SELECT password_hash FROM app_user LIMIT 1)",
            "1",
            "copy_code",
            "e.copy_code",
            "l.title",
            "password_hash",
            "title2",
            "TITLE"
    })
    void rejectsAnythingOutsideTheAllowlist(String maliciousOrUnknown) {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(maliciousOrUnknown));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> ALLOWLIST.sanitize(pageable))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("error.sort.invalid-field"));
    }

    @Test
    void rejectedFieldNeverReachesTheQuery() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id;DROP TABLE book--"));

        // Nome logico invalido para 400; nenhum Pageable e devolvido, logo nao ha
        // caminho em que o texto do cliente chegue ao ORDER BY.
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> ALLOWLIST.sanitize(pageable));
    }

    @Test
    void translatesLogicalFieldToRealColumn() {
        Pageable sanitized = ALLOWLIST.sanitize(
                PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "copyCode")));

        assertThat(sanitized.getPageNumber()).isEqualTo(2);
        assertThat(sanitized.getPageSize()).isEqualTo(15);
        Sort.Order order = sanitized.getSort().getOrderFor("e.copy_code");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sanitized.getSort().getOrderFor("copyCode")).isNull();
    }

    @Test
    void keepsEveryRequestedOrderInOrder() {
        Pageable sanitized = ALLOWLIST.sanitize(PageRequest.of(0, 20,
                Sort.by(Sort.Direction.ASC, "title").and(Sort.by(Sort.Direction.DESC, "copyCode"))));

        assertThat(sanitized.getSort()).extracting(Sort.Order::getProperty)
                .containsExactly("l.title", "e.copy_code");
    }

    @Test
    void unsortedPageablePassesThroughUntouched() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThat(ALLOWLIST.sanitize(pageable)).isSameAs(pageable);
    }

    @Test
    void nullPageablePassesThrough() {
        assertThat(ALLOWLIST.sanitize(null)).isNull();
    }

    @Test
    void errorMessageArgsCarryFieldAndAllowedList() {
        BusinessRuleException error = null;
        try {
            ALLOWLIST.sanitize(PageRequest.of(0, 20, Sort.by("nope")));
        } catch (BusinessRuleException e) {
            error = e;
        }

        assertThat(error).isNotNull();
        assertThat(error.getMessageArgs()).containsExactly("nope", "title, copyCode");
    }

    @Test
    void factoryRejectsUnpairedDeclaration() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> SortAllowlist.of("title"));
    }
}
