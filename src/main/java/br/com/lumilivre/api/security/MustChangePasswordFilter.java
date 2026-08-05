package br.com.lumilivre.api.security;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.common.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Enquanto o usuário tem {@code must_change_password = true}, bloqueia no
 * servidor qualquer operação que modifique estado (POST/PUT/PATCH/DELETE) — exceto
 * a própria troca de senha — para que a flag não seja apenas um aviso ao cliente.
 *
 * <p>Também barra a <b>leitura de dado pessoal de leitor</b> nesse estado. A
 * senha inicial é a matrícula, que está impressa na carteirinha: sem esse
 * bloqueio, qualquer um que veja a carteirinha de um aluno entra e lê CPF,
 * endereço, telefone e data de nascimento dele. A flag existe justamente porque
 * a credencial é presumida comprometida, então ela também não pode liberar PII.
 *
 * <p>As demais leituras (GET) continuam permitidas para não quebrar o bootstrap
 * do cliente (settings/versão/catálogo) e para não disparar logout indevido nos
 * interceptores web/app que deslogam em 403. O gate visual de troca de senha vive
 * no cliente; este filtro é a rede de segurança do lado servidor.
 */
@Component
@RequiredArgsConstructor
public class MustChangePasswordFilter extends OncePerRequestFilter {

    /** Contrato com o cliente: abrir o gate de senha, não deslogar. */
    static final String PASSWORD_CHANGE_REQUIRED_CODE = "PASSWORD_CHANGE_REQUIRED";

    // Todo o fluxo de autenticação fica isento: login, forgot/reset-password e a
    // própria troca de senha precisam funcionar mesmo com a flag ativa (um token
    // antigo no client não pode impedir um novo login/reset).
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    // Cadastro de leitor: nome completo, CPF, endereço, telefone, nascimento.
    private static final String READER_PATH_PREFIX = "/api/readers";

    // Ranking mostra nome e contagem de leituras, sem PII, e alimenta uma aba
    // que o app abre logo depois do login — fica fora do bloqueio.
    private static final String READER_RANKING_PATH = "/api/readers/ranking";

    private final ObjectMapper objectMapper;
    private final MessageResolver messageResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String path = request.getServletPath();

        if (!path.startsWith(AUTH_PATH_PREFIX)
                && (isStateChanging(request.getMethod()) || isPersonalDataRead(request.getMethod(), path))
                && currentUserMustChangePassword()) {
            writeForbidden(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Envelope padrão da API, e não JSON montado à mão.
     *
     * <p>O corpo anterior tinha os nomes de campo certos mas nascia sem {@code
     * timestamp}, {@code path} e {@code correlationId}, com a mensagem cravada em
     * inglês e sem {@code Content-Language} — ou seja, era o único 403 do sistema
     * que um cliente genérico não conseguia tratar como os outros, e sem
     * correlationId ninguém liga esse 403 a nada no log.
     *
     * <p>O {@code code} permanece: é contrato deliberado com o app, que o usa
     * para abrir o gate de troca de senha em vez de deslogar em 403.
     */
    private void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Locale locale = resolveLocale(request);
        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error(messageResolver.resolve("error.password-change-required.title", locale))
                .message(messageResolver.resolve("error.password-change-required.message", locale))
                .path(request.getRequestURI())
                .correlationId(MDC.get("correlationId"))
                .code(PASSWORD_CHANGE_REQUIRED_CODE)
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Content-Language", locale.toLanguageTag());
        objectMapper.writeValue(response.getWriter(), body);
    }

    /** O filtro roda antes do DispatcherServlet, então o locale vem do header. */
    private Locale resolveLocale(HttpServletRequest request) {
        Locale requested = request.getLocale();
        if (requested != null && "en".equals(requested.getLanguage())) {
            return Locale.forLanguageTag("en-US");
        }
        return Locale.forLanguageTag("pt-BR");
    }

    private boolean isStateChanging(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private boolean isPersonalDataRead(String method, String path) {
        return "GET".equals(method)
                && path.startsWith(READER_PATH_PREFIX)
                && !path.startsWith(READER_RANKING_PATH);
    }

    private boolean currentUserMustChangePassword() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getPrincipal() instanceof CustomUserDetails details
                && Boolean.TRUE.equals(details.getAppUser().getMustChangePassword());
    }
}
