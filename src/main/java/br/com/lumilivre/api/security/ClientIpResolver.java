package br.com.lumilivre.api.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolve o IP real do cliente (requisito do dono).
 *
 * <p>A API roda atrás de um reverse proxy (Render). Com
 * {@code server.forward-headers-strategy=framework} habilitado no
 * {@code application.properties}, o Spring já resolve o cabeçalho
 * {@code X-Forwarded-For} anexado pelo proxy confiável e
 * {@link HttpServletRequest#getRemoteAddr()} devolve o IP real do cliente.
 *
 * <p>Deliberadamente <b>não</b> parseamos {@code X-Forwarded-For} manualmente a
 * partir do request do cliente: o primeiro hop é falsificável e chavear baldes
 * de rate-limit por valor controlado pelo atacante habilita spoofing + DoS de
 * memória. Confiamos apenas no IP resolvido pelo proxy.
 */
@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getRemoteAddr();
        return (ip != null && !ip.isBlank()) ? ip : null;
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
}
