package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Reps;

import java.util.Objects;
import java.util.UUID;

/** Ejercicio prescrito dentro de una rutina (§6 - {@code RoutineExercise}). */
public record RoutineExercise(
        UUID exerciseId,
        int order,
        int targetSets,
        Reps targetReps,
        Integer restSeconds
) {

    public RoutineExercise {
        Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
        if (targetSets <= 0) {
            throw new DomainException.Validation("una prescripcion necesita al menos una serie");
        }
        Objects.requireNonNull(targetReps, "las repeticiones objetivo son obligatorias");
        if (restSeconds != null && restSeconds < 0) {
            throw new DomainException.Validation("el descanso no puede ser negativo");
        }
    }
}
