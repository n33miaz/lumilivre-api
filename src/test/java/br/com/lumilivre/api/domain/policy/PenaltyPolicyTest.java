package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.enums.PenaltyCode;

class PenaltyPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "0, RECORD",
            "1, RECORD",
            "2, WARNING",
            "5, WARNING",
            "6, SUSPENSION",
            "7, SUSPENSION",
            "8, BLOCK",
            "90, BLOCK",
            "91, BAN"
    })
    void calculateReturnsExpectedPenaltyForIntervalBoundaries(long daysLate, PenaltyCode expected) {
        assertThat(PenaltyPolicy.calculate(daysLate)).isEqualTo(expected);
    }

    @Test
    void calculateRejectsNegativeDaysLate() {
        assertThatThrownBy(() -> PenaltyPolicy.calculate(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void isMoreSevereAcceptsAnyCandidateWhenCurrentIsNull() {
        assertThat(PenaltyPolicy.isMoreSevere(PenaltyCode.RECORD, null)).isTrue();
    }

    @Test
    void isMoreSevereReturnsTrueOnlyForHigherSeverity() {
        assertThat(PenaltyPolicy.isMoreSevere(PenaltyCode.BLOCK, PenaltyCode.SUSPENSION)).isTrue();
        assertThat(PenaltyPolicy.isMoreSevere(PenaltyCode.WARNING, PenaltyCode.BLOCK)).isFalse();
        assertThat(PenaltyPolicy.isMoreSevere(PenaltyCode.SUSPENSION, PenaltyCode.SUSPENSION)).isFalse();
    }
}
