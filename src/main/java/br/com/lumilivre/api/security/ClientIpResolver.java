package br.com.lumilivre.api.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolve o IP REAL do cliente atrás do reverse proxy do Render.
 *
 * <p><b>Por que não confiar no {@code getRemoteAddr()} com
 * {@code forward-headers-strategy=framework}:</b> aquela estratégia faz o Spring
 * adotar o <i>primeiro</i> elemento do {@code X-Forwarded-For} como IP do
 * cliente. A borda do Render NÃO remove o {@code X-Forwarded-For} que o cliente
 * envia — ela apenas anexa o IP de quem conectou a ela. Logo o primeiro elemento
 * é controlado pelo atacante: rotacionando esse valor ele criava um balde novo
 * de rate-limit por requisição e furava o limite por completo (confirmado em
 * produção). Por isso a estratégia foi mudada para {@code none} e a resolução é
 * feita aqui.
 *
 * <p><b>Regra:</b> com exatamente uma borda confiável (Render) que anexa o IP de
 * origem à DIREITA, o cliente real é o IP <b>público mais à direita</b> do
 * {@code X-Forwarded-For}. Entradas à esquerda são do cliente (ignoradas) e
 * saltos internos do Render são endereços privados/CGNAT (pulados). Isso é
 * robusto a múltiplos hops internos e não depende de saber os IPs do Render.
 * Sem o cabeçalho, cai no {@code getRemoteAddr()}.
 *
 * <p>Registrado por {@code @Import} no {@link AuthRateLimitFilter} (e não por
 * {@code @Component}): assim existe um único bean, disponível tanto no contexto
 * completo quanto na fatia {@code @WebMvcTest} que carrega o filtro.
 */
public class ClientIpResolver {

    /**
     * Headers de IP ÚNICO que o ingress do Render/Cloudflare preenche com o IP
     * real da conexão (e sobrescreve se o cliente tentar forjar). São a fonte
     * mais confiável; ordem de preferência. O {@code X-Forwarded-For}, por si só,
     * NÃO serve no Render: ele mantém o que o cliente envia na frente dos hops
     * que o proxy anexa (confirmado por teste — rotacionar o XFF furava o limite).
     */
    private static final String[] TRUSTED_IP_HEADERS = {
            "CF-Connecting-IP", "True-Client-IP", "X-Real-IP"
    };

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        for (String header : TRUSTED_IP_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                String candidate = stripPort(value.trim());
                if (isExternal(candidate)) {
                    return candidate;
                }
            }
        }
        // Fallback: IP público mais à direita do X-Forwarded-For (o que a borda
        // anexa). Mantido para proxies que injetam o IP real aqui.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String candidate = stripPort(hops[i].trim());
                if (!candidate.isEmpty() && isExternal(candidate)) {
                    return candidate;
                }
            }
        }
        String remote = request.getRemoteAddr();
        return (remote != null && !remote.isBlank()) ? remote : null;
    }

    /** IP do request corrente (fora de controllers, ex.: aspectos/filtros). */
    public String resolveCurrent() {
        return resolve(currentRequest());
    }

    public HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    /**
     * {@code true} quando o literal é um IP roteável na internet (nem privado,
     * nem loopback, nem link-local, nem CGNAT 100.64/10). Só o cliente externo
     * passa; os saltos internos do proxy são pulados.
     */
    private boolean isExternal(String candidate) {
        InetAddress addr = parseLiteral(candidate);
        if (addr == null) {
            return false;
        }
        return !(addr.isLoopbackAddress()
                || addr.isSiteLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isAnyLocalAddress()
                || isCarrierGradeNat(addr)
                || isUniqueLocalV6(addr));
    }

    /**
     * Converte só LITERAIS de IP — nunca nomes de host. Um nome num
     * {@code X-Forwarded-For} forjado não pode disparar DNS a partir daqui.
     */
    private InetAddress parseLiteral(String value) {
        if (!looksLikeIpLiteral(value)) {
            return null;
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private boolean looksLikeIpLiteral(String value) {
        // IPv4 pontuado ou IPv6 (com ':'); nada que possa ser um hostname.
        boolean ipv6 = value.indexOf(':') >= 0 && value.matches("[0-9A-Fa-f:.%]+");
        boolean ipv4 = value.matches("\\d{1,3}(\\.\\d{1,3}){3}");
        return ipv4 || ipv6;
    }

    /** 100.64.0.0/10 — faixa de NAT de operadora usada em redes internas. */
    private boolean isCarrierGradeNat(InetAddress addr) {
        byte[] b = addr.getAddress();
        return b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xFF) >= 64 && (b[1] & 0xFF) <= 127;
    }

    /** fc00::/7 — Unique Local Address do IPv6 (equivalente a privado). */
    private boolean isUniqueLocalV6(InetAddress addr) {
        byte[] b = addr.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }

    private String stripPort(String value) {
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            return end > 0 ? value.substring(1, end) : value;
        }
        // IPv4:porta — um único ':' separa a porta; IPv6 puro tem vários ':'.
        int colon = value.indexOf(':');
        if (colon > 0 && value.indexOf(':', colon + 1) < 0) {
            return value.substring(0, colon);
        }
        return value;
    }
}
