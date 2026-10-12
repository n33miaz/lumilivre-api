package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * O resolver é a defesa contra o bypass de rate limit por X-Forwarded-For
 * confirmado em produção: o atacante rotacionava o primeiro elemento do header
 * e criava um balde novo por requisição. A regra correta atrás da borda do
 * Render (que anexa o IP de origem à direita) é tomar o IP PÚBLICO mais à
 * direita; entradas à esquerda são do cliente e devem ser ignoradas.
 */
class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    private MockHttpServletRequest req(String remoteAddr, String xff) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRemoteAddr(remoteAddr);
        if (xff != null) {
            r.addHeader("X-Forwarded-For", xff);
        }
        return r;
    }

    @Test
    void preferirCfConnectingIpSobreForwardedFor() {
        // No Render (atrás do Cloudflare) o IP real vem no CF-Connecting-IP, que
        // a borda sobrescreve. Ele vence o X-Forwarded-For forjável. Regressão do
        // bypass confirmado e fechado em produção.
        MockHttpServletRequest r = req("0:0:0:0:0:0:0:1", "203.0.113.1, 104.23.254.232, 10.28.211.133");
        r.addHeader("CF-Connecting-IP", "179.191.73.66");
        assertThat(resolver.resolve(r)).isEqualTo("179.191.73.66");
    }

    @Test
    void rotacionarForwardedForNaoMudaOIpQuandoHaCfConnectingIp() {
        MockHttpServletRequest a = req("0:0:0:0:0:0:0:1", "9.9.9.9, 104.23.254.232");
        a.addHeader("CF-Connecting-IP", "179.191.73.66");
        MockHttpServletRequest b = req("0:0:0:0:0:0:0:1", "8.8.8.8, 104.23.254.232");
        b.addHeader("CF-Connecting-IP", "179.191.73.66");
        assertThat(resolver.resolve(a)).isEqualTo("179.191.73.66");
        assertThat(resolver.resolve(b)).isEqualTo("179.191.73.66");
    }

    @Test
    void trueClientIpEXRealIpComoFallbackAntesDoForwardedFor() {
        MockHttpServletRequest tci = req("10.0.0.1", "203.0.113.1");
        tci.addHeader("True-Client-IP", "198.51.100.9");
        assertThat(resolver.resolve(tci)).isEqualTo("198.51.100.9");

        MockHttpServletRequest xri = req("10.0.0.1", "203.0.113.1");
        xri.addHeader("X-Real-IP", "198.51.100.10");
        assertThat(resolver.resolve(xri)).isEqualTo("198.51.100.10");
    }

    @Test
    void semForwardedUsaRemoteAddr() {
        assertThat(resolver.resolve(req("198.51.100.5", null))).isEqualTo("198.51.100.5");
    }

    @Test
    void comUmPublicoRetornaEle() {
        assertThat(resolver.resolve(req("10.0.0.1", "203.0.113.9"))).isEqualTo("203.0.113.9");
    }

    @Test
    void ignoraEntradaForjadaAEsquerdaEPegaORightmostPublico() {
        // O atacante forja "203.0.113.1" à esquerda; o Render anexa o IP real
        // "198.51.100.77" à direita. Deve vencer o rightmost.
        assertThat(resolver.resolve(req("10.0.0.1", "203.0.113.1, 198.51.100.77")))
                .isEqualTo("198.51.100.77");
    }

    @Test
    void rotacionarAEsquerdaNaoMudaOResultado() {
        String a = resolver.resolve(req("10.0.0.1", "1.2.3.4, 198.51.100.77"));
        String b = resolver.resolve(req("10.0.0.1", "9.9.9.9, 198.51.100.77"));
        assertThat(a).isEqualTo("198.51.100.77");
        assertThat(b).isEqualTo("198.51.100.77");
    }

    @Test
    void pulaHopsPrivadosACGNATAEsquerdaDoCliente() {
        // Cliente real seguido de hops internos do proxy (privado + CGNAT) à
        // direita — pula os internos e volta ao cliente público.
        assertThat(resolver.resolve(req("10.0.0.1", "198.51.100.77, 10.1.2.3, 100.64.5.6")))
                .isEqualTo("198.51.100.77");
    }

    @Test
    void nomeDeHostNoHeaderNaoDisparaDnsEEIgnorado() {
        // Um hostname forjado nunca é resolvido (sem DNS); cai no público válido.
        assertThat(resolver.resolve(req("10.0.0.1", "evil.attacker.example, 198.51.100.77")))
                .isEqualTo("198.51.100.77");
    }

    @Test
    void soInternosCaiNoRemoteAddr() {
        assertThat(resolver.resolve(req("172.16.0.9", "10.0.0.1, 192.168.0.2")))
                .isEqualTo("172.16.0.9");
    }

    @Test
    void ipv6PublicoComPortaEntreColchetes() {
        assertThat(resolver.resolve(req("10.0.0.1", "[2001:db8::1]:443")))
                .isEqualTo("2001:db8::1");
    }
}
