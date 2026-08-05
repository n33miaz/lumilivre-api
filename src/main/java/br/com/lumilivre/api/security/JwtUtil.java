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

@Component
public class JwtUtil {

    /** Claim privada com a geração de tokens da conta (V7: app_user.token_version). */
    static final String TOKEN_VERSION_CLAIM = "tver";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

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
