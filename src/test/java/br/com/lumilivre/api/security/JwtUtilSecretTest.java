package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A checagem do segredo na subida.
 *
 * <p>O caso que originou estes testes: em producao o segredo era curto demais, a
 * chave HS256 so e derivada na hora de assinar, e o resultado foi uma aplicacao
 * que subia, respondia UP no health, era marcada como deploy bem-sucedido — e
 * devolvia 500 em <b>todo</b> login. Do lado de fora parecia senha errada.
 */
class JwtUtilSecretTest {

    private static JwtUtil withSecret(String secret) {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "secret", secret);
        ReflectionTestUtils.setField(util, "expiration", 3_600_000L);
        return util;
    }

    @Test
    @DisplayName("o segredo com o tamanho minimo passa")
    void aceitaSegredoNoLimite() {
        String noLimite = "a".repeat(JwtUtil.MIN_SECRET_BYTES);

        assertThatCode(() -> withSecret(noLimite).assertSecretIsStrongEnough())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 16, JwtUtil.MIN_SECRET_BYTES - 1})
    @DisplayName("qualquer segredo abaixo do minimo impede a subida")
    void recusaSegredoCurto(int length) {
        JwtUtil util = withSecret("a".repeat(length));

        assertThatThrownBy(util::assertSecretIsStrongEnough)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LUMILIVRE_JWT_SECRET");
    }

    @Test
    @DisplayName("segredo ausente tambem impede a subida")
    void recusaSegredoNulo() {
        JwtUtil util = withSecret(null);

        assertThatThrownBy(util::assertSecretIsStrongEnough)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("a mensagem diz o tamanho recebido e nunca o valor do segredo")
    void naoVazaOSegredoNaMensagem() {
        String curtoEIdentificavel = "segredo-curto-demais";

        assertThatThrownBy(() -> withSecret(curtoEIdentificavel).assertSecretIsStrongEnough())
                .satisfies(erro -> {
                    assertThat(erro).hasMessageContaining(String.valueOf(curtoEIdentificavel.length()));
                    // O rastro de uma subida falha costuma parar em log e em painel
                    // de deploy; o segredo nao pode viajar junto.
                    assertThat(erro.getMessage()).doesNotContain(curtoEIdentificavel);
                });
    }

    @Test
    @DisplayName("o contador e de bytes, nao de caracteres")
    void contaBytesEmVezDeCaracteres() {
        // 20 caracteres acentuados = 40 bytes em UTF-8. Contar caracteres
        // reprovaria um segredo que o HS256 aceita.
        String acentuado = "á".repeat(20);

        assertThat(acentuado.length()).isLessThan(JwtUtil.MIN_SECRET_BYTES);
        assertThatCode(() -> withSecret(acentuado).assertSecretIsStrongEnough())
                .doesNotThrowAnyException();
    }
}
