package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Creacion de una rutina. */
public record CreateRoutineRequest(
        @NotNull UUID ownerId,
        @NotBlank String name,
        @NotEmpty List<@Valid ExerciseSpecRequest> exercises
) {

    public record ExerciseSpecRequest(
            @NotNull UUID exerciseId,
            @NotNull Integer targetSets,
            @NotNull Integer targetReps,
            Integer restSeconds
    ) {
    }
}
