package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.enums.StatusLivro;

class BookAvailabilityPolicyTest {

    @Test
    void validateAvailableAllowsAvailableStatus() {
        assertThatCode(() -> BookAvailabilityPolicy.validateAvailable(StatusLivro.AVAILABLE))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = StatusLivro.class, names = "AVAILABLE", mode = EnumSource.Mode.EXCLUDE)
    void validateAvailableRejectsUnavailableStatuses(StatusLivro status) {
        assertThatThrownBy(() -> BookAvailabilityPolicy.validateAvailable(status))
                .isInstanceOf(BookAvailabilityViolationException.class)
                .hasMessageContaining("dispon");
    }

    @Test
    void validateAvailableRejectsNullStatus() {
        assertThatThrownBy(() -> BookAvailabilityPolicy.validateAvailable(null))
                .isInstanceOf(BookAvailabilityViolationException.class);
    }
}
