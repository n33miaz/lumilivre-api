package br.com.lumilivre.api.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.RequiredTypeException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
public class JwtUtil {

    /** Claim privada com a geração de tokens da conta (V7: app_user.token_version). */
    static final String TOKEN_VERSION_CLAIM = "tver";

    /** HS256 assina com chave de 256 bits; o jjwt recusa qualquer coisa menor. */
    static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Recusa a subida quando o segredo é curto demais para assinar.
     *
     * <p>Sem esta checagem o defeito é silencioso e caro: a chave só é derivada
     * na hora de assinar, então a aplicação sobe inteira, o {@code /actuator/health}
     * responde UP, o host marca o deploy como bem-sucedido — e <b>todo</b> login
     * devolve 500, porque {@link Keys#hmacShaKeyFor} lança em cada requisição.
     * Aconteceu em produção: o serviço passou a se declarar saudável sem
     * conseguir autenticar ninguém, e do lado de fora parecia erro de senha.
     *
     * <p>Falhar aqui transforma isso num deploy vermelho, com a causa escrita.
     * A mensagem diz o tamanho recebido, nunca o valor.
     */
    @PostConstruct
    void assertSecretIsStrongEnough() {
        int length = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret tem " + length + " bytes e o HS256 exige no minimo " + MIN_SECRET_BYTES
                            + ". Defina LUMILIVRE_JWT_SECRET com pelo menos " + MIN_SECRET_BYTES
                            + " caracteres no ambiente de execucao.");
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Emite o token já carimbado com a geração de tokens da conta
     * ({@code app_user.token_version}). É essa claim que o
     * {@link JwtAuthenticationFilter} confere para decidir se o token foi
     * revogado.
     */
    public String generateToken(UserDetails userDetails, int tokenVersion) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Date issuedAt = new Date();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .claim(TOKEN_VERSION_CLAIM, tokenVersion)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Geração de tokens gravada no token. Devolve {@code null} quando a claim
     * não existe (token emitido antes da V7) ou não é numérica — o chamador
     * trata isso como token não confiável.
     */
    public Integer getTokenVersionFromToken(String token) {
        try {
            return extractClaim(token, claims -> claims.get(TOKEN_VERSION_CLAIM, Integer.class));
        } catch (RequiredTypeException e) {
            return null;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}
