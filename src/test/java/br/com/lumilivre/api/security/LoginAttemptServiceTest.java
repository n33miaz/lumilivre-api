package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A trava por conta é o que sobra quando o atacante troca de IP: o limite do
 * {@link AuthRateLimitFilter} conta requisições por origem, e esta conta falhas
 * por usuário. Sem ela, cinco máquinas diferentes tentando a mesma matrícula
 * passariam pelos dois filtros.
 */
class LoginAttemptServiceTest {

    private static final String USUARIO = "ada@escola.edu.br";

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(new RateLimitProperties());
    }

    @Test
    void aContaSoTravaNaQuintaFalha() {
        for (int tentativa = 1; tentativa < service.maxAttempts(); tentativa++) {
            service.recordFailure(USUARIO);
            assertThat(service.isBlocked(USUARIO))
                    .as("falha %d de %d", tentativa, service.maxAttempts())
                    .isFalse();
        }

        service.recordFailure(USUARIO);

        assertThat(service.isBlocked(USUARIO)).isTrue();
    }

    /**
     * O cliente precisa do tempo restante para dizer "tente de novo em X" em vez
     * de repetir "senha inválida" — que faria o usuário legítimo achar que
     * esqueceu a senha e pedir recuperação sem necessidade.
     */
    @Test
    void aTravaInformaQuantoFaltaEmSegundos() {
        travar(USUARIO);

        long restante = service.blockedSecondsRemaining(USUARIO);

        assertThat(restante).isPositive().isLessThanOrEqualTo(15 * 60);
    }

    @Test
    void contaSemFalhaNenhumaNaoEstaTravadaENaoTemContagem() {
        assertThat(service.isBlocked("desconhecido@escola.edu.br")).isFalse();
        assertThat(service.blockedSecondsRemaining("desconhecido@escola.edu.br")).isZero();
    }

    @Test
    void algumasFalhasSemChegarAoTetoNaoDeixamRelogioCorrendo() {
        service.recordFailure(USUARIO);
        service.recordFailure(USUARIO);

        assertThat(service.blockedSecondsRemaining(USUARIO)).isZero();
    }

    /**
     * Acertar a senha limpa a contagem. Sem isso, quem erra quatro vezes ao
     * longo do semestre e acerta no meio ficaria a uma falha da trava para
     * sempre.
     */
    @Test
    void oLoginBemSucedidoZeraAContagem() {
        service.recordFailure(USUARIO);
        service.recordFailure(USUARIO);
        service.recordFailure(USUARIO);

        service.recordSuccess(USUARIO);
        service.recordFailure(USUARIO);

        assertThat(service.isBlocked(USUARIO)).isFalse();
    }

    /**
     * A chave é normalizada: o formulário de login manda o que o usuário digitou,
     * e {@code Ada@Escola.edu.br } com espaço no fim é a mesma conta. Se não
     * fosse, o atacante ganharia cinco tentativas por variação de caixa.
     */
    @Test
    void maiusculaEEspacoNaoCriamUmaContaNova() {
        travar("  ADA@Escola.Edu.BR ");

        assertThat(service.isBlocked(USUARIO)).isTrue();
        assertThat(service.isBlocked("ada@escola.edu.br")).isTrue();
    }

    @Test
    void usuarioNuloNaoQuebraNemTravaOsDemais() {
        travar(null);

        assertThat(service.isBlocked(null)).isTrue();
        assertThat(service.isBlocked(USUARIO)).isFalse();
    }

    @Test
    void aTravaDeUmaContaNaoAlcancaAOutra() {
        travar(USUARIO);

        assertThat(service.isBlocked("outro@escola.edu.br")).isFalse();
    }

    /**
     * Já travada, falhar de novo não reinicia a janela: senão o atacante
     * insistindo empurraria o desbloqueio para sempre e transformaria a trava em
     * negação de serviço contra o dono da conta.
     *
     * <p>Com relógio de verdade este teste passava mesmo quando o código
     * <i>empurrava</i>: a diferença entre duas chamadas seguidas é de
     * milissegundos e some no truncamento para segundos. Com o relógio
     * controlado, o tempo anda cinco minutos entre as tentativas e o defeito
     * fica visível.
     */
    @Test
    void insistirComAContaTravadaNaoEmpurraODesbloqueio() {
        RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-03-02T10:00:00Z"));
        LoginAttemptService comRelogio = new LoginAttemptService(new RateLimitProperties(), relogio);
        for (int i = 0; i < comRelogio.maxAttempts(); i++) {
            comRelogio.recordFailure(USUARIO);
        }
        long antes = comRelogio.blockedSecondsRemaining(USUARIO);

        relogio.avancar(Duration.ofMinutes(5));
        comRelogio.recordFailure(USUARIO);

        // Passaram 5 dos 15 minutos: restam 10. Se a insistência reagendasse,
        // voltariam a ser 15.
        assertThat(comRelogio.blockedSecondsRemaining(USUARIO)).isEqualTo(antes - 5 * 60);
        assertThat(comRelogio.isBlocked(USUARIO)).isTrue();
    }

    /** Passado o prazo, a conta destrava sozinha — a trava é temporária. */
    @Test
    void aTravaExpiraSozinhaDepoisDoPrazo() {
        RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2026-03-02T10:00:00Z"));
        LoginAttemptService comRelogio = new LoginAttemptService(new RateLimitProperties(), relogio);
        for (int i = 0; i < comRelogio.maxAttempts(); i++) {
            comRelogio.recordFailure(USUARIO);
        }

        relogio.avancar(Duration.ofMinutes(16));

        assertThat(comRelogio.isBlocked(USUARIO)).isFalse();
    }

    /** Relógio que só anda quando o teste manda. */
    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        private RelogioAjustavel(Instant inicio) {
            this.agora = inicio;
        }

        private void avancar(Duration quanto) {
            agora = agora.plus(quanto);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /**
     * O teto configurado precisa valer de verdade. Sem este teste, a trava por
     * conta continuaria em cinco falhas mesmo com a instância de teste pedindo
     * outro número — e o pentest travaria as contas de demonstração no começo.
     */
    @Test
    void oTetoDeFalhasVemDaConfiguracao() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getAccountLock().setMaxAttempts(2);
        LoginAttemptService configurado = new LoginAttemptService(properties);

        configurado.recordFailure(USUARIO);
        assertThat(configurado.isBlocked(USUARIO)).isFalse();

        configurado.recordFailure(USUARIO);

        assertThat(configurado.isBlocked(USUARIO)).isTrue();
    }

    /**
     * A duração da trava também é configurável, e é ela que o cliente mostra
     * como "tente de novo em X".
     */
    @Test
    void aDuracaoDaTravaVemDaConfiguracao() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.getAccountLock().setMaxAttempts(1);
        properties.getAccountLock().setLockDuration(Duration.ofMinutes(2));
        LoginAttemptService configurado = new LoginAttemptService(properties);

        configurado.recordFailure(USUARIO);

        assertThat(configurado.blockedSecondsRemaining(USUARIO))
                .isPositive()
                .isLessThanOrEqualTo(2 * 60);
    }

    private void travar(String username) {
        for (int i = 0; i < service.maxAttempts(); i++) {
            service.recordFailure(username);
        }
    }
}
