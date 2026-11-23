package com.dafit.domain.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: emisor de tokens JWT. */
public interface TokenIssuer {
    Optional<TokenData> issue(UUID userId, String email, Instant now);
    Optional<TokenData> verify(String token);

    record TokenData(String value, UUID userId, String email, Instant expiresAt) {
    }
}