package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Proyeccion publica de un usuario. Sin secretos ni datos medicos. */
public record UserResponse(
        UUID id,
        String displayName,
        String role,
        String sex,
        LocalDate birthDate,
        java.math.BigDecimal bodyweightKg,
        String preferredUnit,
        String timeZone,
        boolean benchmarkConsent,
        Instant createdAt
) {
}
