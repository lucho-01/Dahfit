package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

/** Alta de un ejercicio en el catalogo. */
public record RegisterExerciseRequest(
        @NotBlank String name,
        @NotNull String equipment,
        @NotNull String loadMode,
        boolean unilateral,
        @NotEmpty Set<String> muscles,
        boolean evaluable,
        BigDecimal bodyweightFactor
) {
}
