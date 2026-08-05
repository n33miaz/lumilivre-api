package br.com.lumilivre.api.domain.policy;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.com.lumilivre.api.exception.custom.MessageKeyedException;

/**
 * Força mínima de senha, validada NO SERVIDOR — a regra do formulário é
 * contornável com um curl.
 *
 * <p>Segue o NIST SP 800-63B: aposta em comprimento + lista de proibidas +
 * dados pessoais, e não em "1 maiúscula, 1 número, 1 símbolo" (que empurra o
 * usuário para {@code Senha@123} e não aumenta a entropia real).
 *
 * <ul>
 *   <li><b>Mínimo de 8</b>: é o piso do NIST para senha escolhida por humano e
 *       o teto do que um aluno do ensino médio memoriza sem colar no caderno.
 *       Mais que isso, num sistema onde a senha inicial é a matrícula, viraria
 *       incentivo a anotar a senha.</li>
 *   <li><b>Máximo de 72</b>: o BCrypt ignora bytes além do 72º. Aceitar 200
 *       caracteres daria falsa sensação de segurança.</li>
 *   <li><b>Dados pessoais</b>: matrícula, CPF, e-mail e nome são públicos dentro
 *       da escola; senha derivada deles é senha conhecida.</li>
 * </ul>
 *
 * <p>Ideia futura registrada aqui de propósito: consultar a base de senhas
 * vazadas (HIBP, k-anonymity por prefixo de SHA-1). Fica fora agora para não
 * criar dependência de rede num caminho de autenticação.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    /** Menor pedaço de dado pessoal que ainda é reconhecível dentro da senha. */
    private static final int MIN_PERSONAL_TOKEN_LENGTH = 4;

    /**
     * Lista curta e embutida das senhas que aparecem em qualquer top-100 de
     * vazamento, mais as óbvias em português e as ligadas ao produto.
     */
    private static final Set<String> FORBIDDEN = Set.of(
            "12345678", "123456789", "1234567890", "123456", "1234567",
            "password", "password1", "password123", "passw0rd",
            "qwerty", "qwertyui", "qwerty123", "asdfghjk", "abcdefgh",
            "iloveyou", "welcome", "welcome1", "admin", "administrator",
            "adminadmin", "letmein", "monkey", "dragon", "football", "sunshine",
            "senha", "senha123", "senha1234", "minhasenha", "mudar123",
            "brasil", "brasil123", "flamengo", "corinthians", "palmeiras",
            "biblioteca", "lumilivre", "lumilivre123", "escola", "aluno",
            "professor", "estudante", "matricula");

    private PasswordPolicy() {}

    /**
     * Valida a nova senha do usuário.
     *
     * @param rawPassword senha em claro escolhida pelo usuário
     * @param currentHashMatches se a nova senha é igual à atual (o hash quem
     *        compara é o encoder, aqui só entra o resultado)
     * @param personalData valores que a senha não pode reproduzir (matrícula,
     *        CPF, e-mail, nome); nulos e vazios são ignorados
     */
    public static void validate(String rawPassword, boolean currentHashMatches, String... personalData) {
        // isBlank() pega os 8+ espaços que passariam pelo teste de comprimento.
        if (rawPassword == null || rawPassword.isBlank() || rawPassword.length() < MIN_LENGTH) {
            throw new PasswordPolicyViolationException("validation.password.too-short");
        }
        if (rawPassword.length() > MAX_LENGTH) {
            throw new PasswordPolicyViolationException("validation.password.too-long");
        }
        if (currentHashMatches) {
            throw new PasswordPolicyViolationException("validation.password.same-as-current");
        }

        String normalized = normalize(rawPassword);

        if (FORBIDDEN.contains(normalized)) {
            throw new PasswordPolicyViolationException("validation.password.too-common");
        }
        if (isSingleRepeatedCharacter(normalized) || isTrivialSequence(normalized)) {
            throw new PasswordPolicyViolationException("validation.password.too-simple");
        }
        if (derivesFromPersonalData(normalized, personalData)) {
            throw new PasswordPolicyViolationException("validation.password.personal-data");
        }
    }

    /**
     * Minúsculas, sem acento e só alfanumérico: {@code Már.Ia2024} e
     * {@code maria2024} são a mesma senha para efeito de "derivada de".
     */
    private static String normalize(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccents.replaceAll("[^a-z0-9]", "");
    }

    private static boolean isSingleRepeatedCharacter(String normalized) {
        return !normalized.isEmpty() && normalized.chars().distinct().count() == 1;
    }

    /** Corridas do teclado/alfabeto/dígitos, para frente ou para trás. */
    private static boolean isTrivialSequence(String normalized) {
        if (normalized.length() < MIN_LENGTH) {
            return false;
        }
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < normalized.length(); i++) {
            int delta = normalized.charAt(i) - normalized.charAt(i - 1);
            if (delta != 1) ascending = false;
            if (delta != -1) descending = false;
        }
        return ascending || descending;
    }

    /**
     * Recusa se a senha contém um dado pessoal ou está contida nele — pega
     * {@code 2024001}, {@code senha2024001} e {@code 2024001x} de uma vez.
     */
    private static boolean derivesFromPersonalData(String normalizedPassword, String... personalData) {
        if (personalData == null) {
            return false;
        }
        for (String datum : personalData) {
            if (datum == null || datum.isBlank()) {
                continue;
            }
            for (String token : splitTokens(datum)) {
                if (token.length() < MIN_PERSONAL_TOKEN_LENGTH) {
                    continue;
                }
                if (normalizedPassword.contains(token) || token.contains(normalizedPassword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * O valor inteiro normalizado mais cada palavra dele: o nome completo
     * dificilmente vira senha, mas o primeiro nome vira.
     */
    private static List<String> splitTokens(String datum) {
        List<String> tokens = new ArrayList<>();
        String whole = normalize(datum);
        if (!whole.isEmpty()) {
            tokens.add(whole);
        }
        for (String part : datum.split("[^\\p{L}\\p{N}]+")) {
            String normalizedPart = normalize(part);
            if (!normalizedPart.isEmpty()) {
                tokens.add(normalizedPart);
            }
        }
        return tokens;
    }

    public static class PasswordPolicyViolationException extends RuntimeException implements MessageKeyedException {
        private final String messageKey;
        private final Object[] messageArgs;

        public PasswordPolicyViolationException(String key, Object... args) {
            super(key);
            this.messageKey = key;
            this.messageArgs = args;
        }

        @Override
        public boolean hasI18nKey() {
            return true;
        }

        @Override
        public String getMessageKey() {
            return messageKey;
        }

        @Override
        public Object[] getMessageArgs() {
            return messageArgs;
        }
    }
}
