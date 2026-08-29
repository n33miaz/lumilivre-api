package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Tornar o rate limit configurável só é seguro se o default continuar sendo a
 * defesa que já existia e se um valor absurdo derrubar a subida em vez de
 * desligar a proteção calado. Estes testes cobrem as duas pontas.
 */
class RateLimitPropertiesTest {

    /**
     * Os números aqui são os que viviam cravados no {@link AuthRateLimitFilter} e
     * no {@link LoginAttemptService}. Se alguém afrouxar um default sem querer —
     * ou "só para o pentest passar" — este teste quebra antes de virar deploy.
     */
    @Test
    void osDefaultsSaoExatamenteOsLimitesQueJaValiam() {
        RateLimitProperties properties = new RateLimitProperties();

        assertThat(properties.getAuth().getCapacity()).isEqualTo(5);
        assertThat(properties.getAuth().getWindow()).isEqualTo(Duration.ofMinutes(10));

        assertThat(properties.getValidateToken().getCapacity()).isEqualTo(5);
        assertThat(properties.getValidateToken().getWindow()).isEqualTo(Duration.ofMinutes(10));

        assertThat(properties.getPublicRead().getCapacity()).isEqualTo(300);
        assertThat(properties.getPublicRead().getWindow()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.getInterestWrite().getCapacity()).isEqualTo(120);
        assertThat(properties.getInterestWrite().getWindow()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.getAccountLock().getMaxAttempts()).isEqualTo(5);
        assertThat(properties.getAccountLock().getWindow()).isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getAccountLock().getLockDuration()).isEqualTo(Duration.ofMinutes(15));
    }

    /**
     * Capacidade zero seria a forma mais discreta de desligar o rate limit: a
     * app subiria normalmente e o balde nunca deixaria passar — ou, pior, com um
     * bucket4j de capacidade nula, o comportamento vira indefinido. Recusar é
     * melhor que adivinhar.
     */
    @Test
    void capacidadeZeroOuNegativaNaoPassaNaValidacao() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getAuth().setCapacity(0);

        assertThat(violacoes(properties))
                .anyMatch(v -> v.getPropertyPath().toString().equals("auth.capacity"));
    }

    @Test
    void janelaZeradaNaoPassaNaValidacao() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getPublicRead().setWindow(Duration.ZERO);

        assertThat(violacoes(properties))
                .anyMatch(v -> v.getPropertyPath().toString().equals("publicRead.windowPositive"));
    }

    @Test
    void travaPorContaComDuracaoNegativaNaoPassaNaValidacao() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getAccountLock().setLockDuration(Duration.ofMinutes(-1));

        assertThat(violacoes(properties))
                .anyMatch(v -> v.getPropertyPath().toString().equals("accountLock.lockDurationPositive"));
    }

    @Test
    void aConfiguracaoPadraoNaoTemNenhumaViolacao() {
        assertThat(violacoes(new RateLimitProperties())).isEmpty();
    }

    private static Set<ConstraintViolation<RateLimitProperties>> violacoes(RateLimitProperties properties) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(properties);
        }
    }
}
