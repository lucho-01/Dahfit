package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proyeccion publica de una rutina y sus prescripciones. */
public record RoutineResponse(
        UUID id,
        UUID ownerId,
        UUID assignedToId,
        String name,
        List<ExerciseSpecResponse> exercises,
        Instant createdAt
) {

    public record ExerciseSpecResponse(
            UUID exerciseId,
            int order,
            int targetSets,
            int targetReps,
            Integer restSeconds
    ) {
    }
}
