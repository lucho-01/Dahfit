package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Rutina (§6 - {@code Routine}).
 *
 * <p>Puede ser propia o asignada por un entrenador. La asignacion exige
 * consentimiento revocable (§9); aqui solo se modela el destinatario.
 */
public record Routine(
        UUID id,
        UUID ownerId,
        UUID assignedToId,
        String name,
        List<RoutineExercise> exercises,
        Instant createdAt
) {

    public Routine {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(ownerId, "el propietario es obligatorio");
        Objects.requireNonNull(createdAt, "la fecha de creacion es obligatoria");
        if (name == null || name.isBlank()) {
            throw new DomainException.Validation("la rutina necesita un nombre");
        }
        exercises = exercises == null ? List.of() : List.copyOf(exercises);
    }

    public static Routine create(UUID ownerId, String name, List<RoutineExercise> exercises, Instant now) {
        return new Routine(UUID.randomUUID(), ownerId, null, name, exercises, now);
    }

    public Routine assignTo(UUID athleteId) {
        if (athleteId == null) {
            throw new DomainException.Validation("la rutina necesita un destinatario");
        }
        return new Routine(id, ownerId, athleteId, name, exercises, createdAt);
    }

    public Routine duplicateFor(UUID newOwnerId, String newName) {
        return new Routine(UUID.randomUUID(), newOwnerId, null, newName, exercises, Instant.now());
    }

    public List<RoutineExercise> orderedExercises() {
        return exercises.stream()
                .sorted(Comparator.comparingInt(RoutineExercise::order))
                .toList();
    }

    public Optional<RoutineExercise> findExercise(UUID exerciseId) {
        return exercises.stream().filter(e -> e.exerciseId().equals(exerciseId)).findFirst();
    }
}
