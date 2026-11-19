package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Proyeccion publica de una pesada del historico corporal. */
public record BodyMetricResponse(
        UUID id,
        UUID userId,
        BigDecimal weightKg,
        LocalDate date,
        Instant recordedAt,
        String source
) {
}
