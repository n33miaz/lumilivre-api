package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import br.com.lumilivre.api.enums.Penalidade;

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
    void calculateReturnsExpectedPenaltyForIntervalBoundaries(long daysLate, Penalidade expected) {
        assertThat(PenaltyPolicy.calculate(daysLate)).isEqualTo(expected);
    }

    @Test
    void calculateRejectsNegativeDaysLate() {
        assertThatThrownBy(() -> PenaltyPolicy.calculate(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void isMoreSevereAcceptsAnyCandidateWhenCurrentIsNull() {
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.RECORD, null)).isTrue();
    }

    @Test
    void isMoreSevereReturnsTrueOnlyForHigherSeverity() {
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.BLOCK, Penalidade.SUSPENSION)).isTrue();
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.WARNING, Penalidade.BLOCK)).isFalse();
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.SUSPENSION, Penalidade.SUSPENSION)).isFalse();
    }
}
