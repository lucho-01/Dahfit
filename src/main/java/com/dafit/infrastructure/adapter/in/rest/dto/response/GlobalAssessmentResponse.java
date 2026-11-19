package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Rango general.
 *
 * <p>Cuando no es oficial, {@code coveragePercent} comunica el progreso (7/8 =
 * 88 %), nunca un rango presentado como oficial.
 */
public record GlobalAssessmentResponse(
        UUID id,
        UUID userId,
        String status,
        Integer points,
        String rank,
        int musclesEvaluated,
        int musclesRequired,
        int coveragePercent,
        Instant assessedAt
) {
}
