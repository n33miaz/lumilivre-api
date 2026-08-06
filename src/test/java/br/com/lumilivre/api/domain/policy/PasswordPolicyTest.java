package br.com.lumilivre.api.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.lumilivre.api.domain.policy.PasswordPolicy.PasswordPolicyViolationException;

class PasswordPolicyTest {

    private static final String REGISTRATION = "2024001";
    private static final String CPF = "12345678901";
    private static final String EMAIL = "maria.souza@escola.test";
    private static final String FULL_NAME = "Maria Souza Lima";

    @Test
    void acceptsPasswordThatIsLongEnoughAndUnrelatedToTheAccount() {
        assertThatCode(() -> PasswordPolicy.validate("chuva-de-papel-77", false, personalData()))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", "abc", "1234567", "sete123" })
    void rejectsPasswordShorterThanTheMinimum(String weak) {
        assertViolation(weak, "validation.password.too-short");
    }

    @Test
    void rejectsPasswordLongerThanWhatBcryptActuallyHashes() {
        assertViolation("a1!".repeat(30), "validation.password.too-long");
    }

    @ParameterizedTest
    @ValueSource(strings = { "12345678", "password", "qwerty123", "senha123", "biblioteca", "lumilivre123" })
    void rejectsWellKnownPasswords(String common) {
        assertViolation(common, "validation.password.too-common");
    }

    @ParameterizedTest
    @ValueSource(strings = { "aaaaaaaa", "abcdefghij", "jihgfedcba", "87654321" })
    void rejectsRepeatedCharactersAndKeyboardRuns(String simple) {
        assertViolation(simple, "validation.password.too-simple");
    }

    @Test
    void rejectsPasswordEqualToTheRegistrationNumber() {
        // A senha inicial do leitor E a matricula: e exatamente o que a politica
        // tem de impedir de continuar valendo depois da troca obrigatoria.
        assertViolation("2024001", "validation.password.too-short");
        assertViolation("20240012024001", "validation.password.personal-data");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "senha2024001",          // matrícula embutida
            "12345678901",           // CPF puro
            "maria.souza2024",       // parte do e-mail
            "MariaSouza!",           // nome do leitor
            "escola-teste-1",        // domínio do e-mail
            "lima-lima-lima"         // sobrenome
    })
    void rejectsPasswordDerivedFromRegistrationData(String derived) {
        assertViolation(derived, "validation.password.personal-data");
    }

    @Test
    void ignoresAccentsAndPunctuationWhenComparingWithRegistrationData() {
        assertViolation("M-á-r-i-a-2024", "validation.password.personal-data",
                "2024001", null, null, "Maria Souza Lima");
    }

    @Test
    void rejectsReusingTheCurrentPassword() {
        assertThatExceptionOfType(PasswordPolicyViolationException.class)
                .isThrownBy(() -> PasswordPolicy.validate("chuva-de-papel-77", true, personalData()))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("validation.password.same-as-current"));
    }

    @Test
    void ignoresNullAndBlankPersonalData() {
        assertThatCode(() -> PasswordPolicy.validate("chuva-de-papel-77", false, null, "", "  "))
                .doesNotThrowAnyException();
    }

    @Test
    void shortPersonalDataDoesNotBlockUnrelatedPasswords() {
        // Tokens com menos de 4 caracteres (ex.: "br" do dominio) nao podem
        // condenar toda senha que contenha essas duas letras.
        assertThatCode(() -> PasswordPolicy.validate("bruma-alta-2199", false, "ana@x.br"))
                .doesNotThrowAnyException();
    }

    /**
     * Senha nula é recusada como curta demais, não como {@code NullPointer}. O
     * caminho existe: o corpo de {@code change-password} é JSON do cliente, e
     * campo ausente chega nulo antes de qualquer validação de bean.
     */
    @Test
    void aNullPasswordIsRefusedAndDoesNotBlowUp() {
        assertViolation(null, "validation.password.too-short");
    }

    /**
     * A comparação com dado pessoal roda sobre a senha normalizada (sem acento,
     * sem pontuação). Uma senha só de símbolos normaliza para string vazia — o
     * que não pode ser lido como "repetição de um caractere" nem derrubar a
     * política. Sem dado pessoal para comparar, ela passa pelo comprimento.
     */
    @Test
    void aPasswordMadeOnlyOfSymbolsIsNotMistakenForATrivialOne() {
        assertThatCode(() -> PasswordPolicy.validate("@#$%&*()!", false))
                .doesNotThrowAnyException();
    }

    /**
     * Chamador sem nenhum dado pessoal (o array inteiro nulo) é caso real: a
     * troca de senha do administrador não tem matrícula nem CPF para comparar.
     */
    @Test
    void aCallerWithoutAnyPersonalDataStillGetsTheOtherRules() {
        assertThatCode(() -> PasswordPolicy.validate("chuva-de-papel-77", false, (String[]) null))
                .doesNotThrowAnyException();
        assertThatExceptionOfType(PasswordPolicyViolationException.class)
                .isThrownBy(() -> PasswordPolicy.validate("senha123", false, (String[]) null));
    }

    /**
     * Dado pessoal que não tem letra nem dígito não gera token nenhum — e sem
     * token, não pode condenar senha alguma. Se gerasse um token vazio, toda
     * senha do sistema seria recusada como "derivada de dado pessoal".
     */
    @Test
    void personalDataWithoutLettersOrDigitsBlocksNothing() {
        assertThatCode(() -> PasswordPolicy.validate("chuva-de-papel-77", false, "---", "..."))
                .doesNotThrowAnyException();
    }

    private static void assertViolation(String password, String expectedKey) {
        assertViolation(password, expectedKey, personalData());
    }

    private static void assertViolation(String password, String expectedKey, String... personalData) {
        assertThatExceptionOfType(PasswordPolicyViolationException.class)
                .isThrownBy(() -> PasswordPolicy.validate(password, false, personalData))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo(expectedKey));
    }

    private static String[] personalData() {
        return new String[] { EMAIL, REGISTRATION, CPF, EMAIL, FULL_NAME };
    }
}
