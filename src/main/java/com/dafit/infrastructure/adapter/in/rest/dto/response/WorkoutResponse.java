package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Proyeccion publica de una sesion de entrenamiento. */
public record WorkoutResponse(
        UUID id,
        UUID userId,
        LocalDate localDate,
        String timeZone,
        Instant startedAt,
        Instant finishedAt,
        String status,
        List<WorkoutSetResponse> sets,
        String notes,
        BigDecimal totalVolumeKg
) {
}
