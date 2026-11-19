package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

/** Token de acceso emitido por el login. */
public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UUID userId
) {
}
