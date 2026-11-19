package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Proyeccion publica de un ejercicio del catalogo. */
public record ExerciseResponse(
        UUID id,
        String name,
        String equipment,
        String loadMode,
        boolean unilateral,
        List<String> muscles,
        boolean evaluable,
        BigDecimal bodyweightFactor,
        boolean active
) {
}
