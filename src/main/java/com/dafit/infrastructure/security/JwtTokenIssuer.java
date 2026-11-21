package com.dafit.infrastructure.security;

import com.dafit.domain.port.out.TokenIssuer;
import com.dafit.infrastructure.config.DafitProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT HS256 firmados con {@code dafit.security.jwt-secret}.
 *
 * <p>El secreto se valida de forma perezosa: si la seguridad esta apagada, el
 * contexto arranca sin secreto. Si esta encendida, {@code SecurityConfig} falla
 * al arrancar cuando falta o es demasiado corto, para no firmar con una clave
 * debil en produccion.
 */
@Component
public class JwtTokenIssuer implements TokenIssuer {

    static final int MIN_SECRET_BYTES = 32;

    private final String secret;
    private final Duration ttl;

    public JwtTokenIssuer(DafitProperties properties) {
        this.secret = properties.security().jwtSecret();
        this.ttl = Duration.ofMinutes(properties.security().jwtTtlMinutes() > 0
                ? properties.security().jwtTtlMinutes() : 60);
    }

    @Override
    public Optional<Token> issue(UUID userId, String email, Instant now) {
        SecretKey key = key();
        Instant expiresAt = now.plus(ttl);

        String value = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return Optional.of(new Token(value, userId, email, expiresAt));
    }

    @Override
    public Optional<Token> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(new Token(
                    token,
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.getExpiration().toInstant()));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private SecretKey key() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "DAFIT_JWT_SECRET debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
