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
            "0, REGISTRO",
            "1, REGISTRO",
            "2, ADVERTENCIA",
            "5, ADVERTENCIA",
            "6, SUSPENSAO",
            "7, SUSPENSAO",
            "8, BLOQUEIO",
            "90, BLOQUEIO",
            "91, BANIMENTO"
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
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.REGISTRO, null)).isTrue();
    }

    @Test
    void isMoreSevereReturnsTrueOnlyForHigherSeverity() {
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.BLOQUEIO, Penalidade.SUSPENSAO)).isTrue();
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.ADVERTENCIA, Penalidade.BLOQUEIO)).isFalse();
        assertThat(PenaltyPolicy.isMoreSevere(Penalidade.SUSPENSAO, Penalidade.SUSPENSAO)).isFalse();
    }
}
