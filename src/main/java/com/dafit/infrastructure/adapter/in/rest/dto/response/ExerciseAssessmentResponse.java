package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evaluacion de un ejercicio.
 *
 * <p>Los campos de resultado son nulos cuando el estado no es oficial: el
 * estado lo explica todo y {@code non_null} los omite del JSON.
 */
public record ExerciseAssessmentResponse(
        UUID id,
        UUID userId,
        UUID exerciseId,
        String status,
        Integer points,
        String rank,
        BigDecimal e1rmKg,
        BigDecimal relativeStrength,
        BigDecimal percentile,
        String algorithmVersion,
        UUID benchmarkTableId,
        BigDecimal bodyweightUsedKg,
        UUID bestSetId,
        Instant assessedAt
) {
}
