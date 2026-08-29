package br.com.lumilivre.api.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tetos do rate limit por IP e da trava por conta, ligados a
 * {@code lumilivre.rate-limit.*}.
 *
 * <p>Os defaults são exatamente os números que viviam cravados no
 * {@link AuthRateLimitFilter} e no {@link LoginAttemptService}: um ambiente que
 * não configura nada continua se defendendo igual, e a mudança não é um
 * afrouxamento disfarçado.
 *
 * <p>O motivo de existir é o inverso — poder <b>apertar ou afrouxar uma
 * instância de teste</b> por variável de ambiente. Um pentest automatizado
 * esbarra no 429 nos primeiros segundos (cinco requisições de auth por IP a
 * cada dez minutos) e nunca alcança o resto da superfície; antes disso, a única
 * saída era editar constante compilada, com o risco óbvio de a edição viajar
 * para produção num commit distraído.
 *
 * <p>Validado na subida porque capacidade zero ou janela negativa desligariam a
 * defesa <i>em silêncio</i>: a aplicação recusa iniciar em vez de servir sem
 * limite nenhum.
 *
 * <p>Registrada por {@code @EnableConfigurationProperties} em quem consome, e
 * não por {@code @Component}: os testes de fatia ({@code @WebMvcTest}) puxam o
 * filtro — que é um {@code Filter} — mas não fariam varredura de componente
 * atrás desta classe, e o contexto da fatia quebraria.
 */
@ConfigurationProperties(prefix = "lumilivre.rate-limit")
@Validated
@Data
public class RateLimitProperties {

    /** Login, recuperação e troca de senha: adivinhação de credencial. */
    @Valid
    @NotNull
    private Bucket auth = new Bucket(5, Duration.ofMinutes(10));

    /** Validação do token de recuperação, em balde próprio. */
    @Valid
    @NotNull
    private Bucket validateToken = new Bucket(5, Duration.ofMinutes(10));

    /** Leituras anônimas do acervo (catálogo, ficha, configuração pública). */
    @Valid
    @NotNull
    private Bucket publicRead = new Bucket(300, Duration.ofMinutes(1));

    /** Marcar e desmarcar interesse: escrita autenticada, balde separado. */
    @Valid
    @NotNull
    private Bucket interestWrite = new Bucket(120, Duration.ofMinutes(1));

    /** Trava por conta, que é o que sobra quando o atacante troca de IP. */
    @Valid
    @NotNull
    private AccountLock accountLock = new AccountLock();

    /** Um balde: quantas requisições cabem, e em quanto tempo ele reenche. */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Bucket {

        @Min(value = 1, message = "capacidade de rate limit precisa ser pelo menos 1")
        private int capacity;

        @NotNull
        private Duration window;

        @AssertTrue(message = "janela de rate limit precisa ser positiva")
        public boolean isWindowPositive() {
            return window != null && !window.isNegative() && !window.isZero();
        }
    }

    /** Falhas toleradas por conta antes do bloqueio, e por quanto tempo ele dura. */
    @Data
    public static class AccountLock {

        @Min(value = 1, message = "número de tentativas precisa ser pelo menos 1")
        private int maxAttempts = 5;

        /** Janela em que as falhas se somam. */
        @NotNull
        private Duration window = Duration.ofMinutes(15);

        /** Quanto tempo a conta fica bloqueada depois de estourar. */
        @NotNull
        private Duration lockDuration = Duration.ofMinutes(15);

        @AssertTrue(message = "janela da trava por conta precisa ser positiva")
        public boolean isWindowPositive() {
            return window != null && !window.isNegative() && !window.isZero();
        }

        @AssertTrue(message = "duração da trava por conta precisa ser positiva")
        public boolean isLockDurationPositive() {
            return lockDuration != null && !lockDuration.isNegative() && !lockDuration.isZero();
        }
    }
}
