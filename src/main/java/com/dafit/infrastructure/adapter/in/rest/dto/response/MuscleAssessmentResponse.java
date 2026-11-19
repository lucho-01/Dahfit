package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Evaluacion de un grupo muscular. */
public record MuscleAssessmentResponse(
        UUID id,
        UUID userId,
        String muscle,
        String status,
        Integer points,
        String rank,
        BigDecimal coverage,
        List<UUID> exercisesEvaluated,
        int primaryTestsEvaluated,
        Instant assessedAt
) {
}
