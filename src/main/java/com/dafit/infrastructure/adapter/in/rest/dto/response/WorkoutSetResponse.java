package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Proyeccion publica de una serie de entrenamiento. */
public record WorkoutSetResponse(
        UUID id,
        UUID exerciseId,
        int order,
        BigDecimal loadKg,
        String inputUnit,
        String loadMode,
        int implementCount,
        int reps,
        int rir,
        String type,
        String notes,
        Instant performedAt,
        BigDecimal totalLoadKg,
        BigDecimal bodyweightKg,
        BigDecimal bodyweightFactor
) {
}
