package br.com.lumilivre.api.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica pelo Bearer JWT e é o ponto onde a revogação acontece.
 *
 * <p>Assinatura válida e prazo não expirado não bastam: o token também precisa
 * carregar a geração vigente da conta ({@code app_user.token_version}) e
 * pertencer a uma conta ativa, não bloqueada e não excluída. Sem isso, demitir
 * alguém ou trocar a senha não tirava o acesso até o token vencer.
 *
 * <p>Quando qualquer verificação falha o filtro simplesmente não popula o
 * contexto — a requisição segue anônima e o entry point devolve 401.
 *
 * <p>Roda em toda rota: {@code POST /api/auth/logout} precisa do principal
 * resolvido. O {@code shouldNotFilter} anterior comparava com o prefixo
 * {@code /auth/}, que nenhuma rota real tem ({@code @RequestMapping} é
 * {@code /api/auth}), então nunca isentou nada.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractJwtFromRequest(request);

        if (token != null && jwtUtil.validateToken(token)) {
            authenticate(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(jwtUtil.getUsernameFromToken(token));
        } catch (UsernameNotFoundException e) {
            // Conta apagada/excluída depois da emissão do token: segue anônimo.
            return;
        }

        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            return;
        }

        if (userDetails instanceof CustomUserDetails details
                && isRevoked(token, details.getAppUser().getTokenVersion())) {
            return;
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Compara a geração gravada no token com a da conta: igualdade exata.
     *
     * <p>Não há relógio envolvido, e por isso não há janela: um token revogado
     * no mesmo instante em que foi emitido já não bate mais. Qualquer outra
     * situação — claim ausente (token anterior à V7), claim não numérica,
     * coluna nula — falha fechado.
     */
    private boolean isRevoked(String token, Integer accountTokenVersion) {
        Integer tokenVersion = jwtUtil.getTokenVersionFromToken(token);
        return tokenVersion == null
                || accountTokenVersion == null
                || !tokenVersion.equals(accountTokenVersion);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
