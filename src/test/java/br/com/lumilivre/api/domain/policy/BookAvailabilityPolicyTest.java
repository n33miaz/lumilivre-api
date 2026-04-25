package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.enums.BookCopyStatus;

class BookAvailabilityPolicyTest {

    @Test
    void validateAvailableAllowsAvailableStatus() {
        assertThatCode(() -> BookAvailabilityPolicy.validateAvailable(BookCopyStatus.AVAILABLE))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = BookCopyStatus.class, names = "AVAILABLE", mode = EnumSource.Mode.EXCLUDE)
    void validateAvailableRejectsUnavailableStatuses(BookCopyStatus status) {
        assertThatThrownBy(() -> BookAvailabilityPolicy.validateAvailable(status))
                .isInstanceOf(BookAvailabilityViolationException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void validateAvailableRejectsNullStatus() {
        assertThatThrownBy(() -> BookAvailabilityPolicy.validateAvailable(null))
                .isInstanceOf(BookAvailabilityViolationException.class);
    }
}
