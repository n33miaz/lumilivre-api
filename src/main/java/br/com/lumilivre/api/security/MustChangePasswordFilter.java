package br.com.lumilivre.api.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Enquanto o usuário tem {@code must_change_password = true}, bloqueia no
 * servidor qualquer operação que modifique estado (POST/PUT/PATCH/DELETE) — exceto
 * a própria troca de senha — para que a flag não seja apenas um aviso ao cliente.
 *
 * <p>Leituras (GET) continuam permitidas para não quebrar o bootstrap do cliente
 * (settings/versão/etc.) e para não disparar logout indevido nos interceptores
 * web/app que deslogam em 403. O gate visual de troca de senha vive no cliente;
 * este filtro é a rede de segurança do lado servidor.
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    // Todo o fluxo de autenticação fica isento: login, forgot/reset-password e a
    // própria troca de senha precisam funcionar mesmo com a flag ativa (um token
    // antigo no client não pode impedir um novo login/reset).
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        if (isStateChanging(request.getMethod())
                && !request.getServletPath().startsWith(AUTH_PATH_PREFIX)
                && currentUserMustChangePassword()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"Forbidden\","
                            + "\"message\":\"Password change required before performing this action.\","
                            + "\"code\":\"PASSWORD_CHANGE_REQUIRED\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isStateChanging(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean currentUserMustChangePassword() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getPrincipal() instanceof CustomUserDetails details
                && Boolean.TRUE.equals(details.getAppUser().getMustChangePassword());
    }
}
