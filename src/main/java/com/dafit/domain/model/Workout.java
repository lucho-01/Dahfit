package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Sesion de entrenamiento (§6 - {@code Workout}). Agregado raiz de las series.
 *
 * <p>La fecha local y la zona horaria se guardan juntas porque los cortes de
 * "semana" y de ciclo de cuatro semanas se calculan en hora local, no en UTC
 * (C-18). Un entrenamiento del domingo a las 21:00 en Bogota pertenece a esa
 * semana aunque en UTC ya sea lunes.
 */
public record Workout(
        UUID id,
        UUID userId,
        LocalDate localDate,
        ZoneId timeZone,
        Instant startedAt,
        Instant finishedAt,
        WorkoutStatus status,
        List<WorkoutSet> sets,
        String notes,
        Kg bodyweight
) {

    public Workout {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(localDate, "la fecha local es obligatoria");
        Objects.requireNonNull(startedAt, "el inicio es obligatorio");
        timeZone = timeZone == null ? User.DEFAULT_ZONE : timeZone;
        status = status == null ? WorkoutStatus.IN_PROGRESS : status;
        sets = sets == null ? List.of() : List.copyOf(sets);
        if (sets.stream().anyMatch(set -> !set.workoutId().equals(id))) {
            throw new DomainException.Validation("todas las series deben pertenecer a la sesion");
        }
    }

    public static Workout start(UUID userId, LocalDate localDate, ZoneId timeZone, Instant startedAt) {
        return new Workout(
                UUID.randomUUID(), userId, localDate, timeZone, startedAt,
                null, WorkoutStatus.IN_PROGRESS, List.of(), null, null);
    }

    /**
     * Anade una serie de forma idempotente (C-17).
     *
     * <p>Si el id ya existe se devuelve la sesion sin cambios: reenviar una
     * operacion de sincronizacion no puede duplicar series.
     */
    public Workout addSet(WorkoutSet set) {
        if (sets.stream().anyMatch(existing -> existing.id().equals(set.id()))) {
            return this;
        }
        List<WorkoutSet> updated = new ArrayList<>(sets);
        updated.add(set.withOrder(updated.size() + 1));
        updated.sort(Comparator.comparingInt(WorkoutSet::order));
        return new Workout(id, userId, localDate, timeZone, startedAt, finishedAt, status, updated, notes, bodyweight);
    }

    public Workout finish(Instant at) {
        if (status == WorkoutStatus.COMPLETED) {
            throw new DomainException.Validation("la sesion ya esta cerrada");
        }
        return new Workout(id, userId, localDate, timeZone, startedAt, at,
                WorkoutStatus.COMPLETED, sets, notes, bodyweight);
    }

    public Workout withNotes(String newNotes) {
        return new Workout(id, userId, localDate, timeZone, startedAt, finishedAt, status, sets, newNotes, bodyweight);
    }

    /** Peso corporal del dia; lo usa la carga total de las series de peso corporal (C-08). */
    public Workout withBodyweight(Kg bodyweight) {
        return new Workout(id, userId, localDate, timeZone, startedAt, finishedAt, status,
                sets, notes, bodyweight);
    }

    public Kg bodyweight() {
        return bodyweight;
    }

    public boolean isInProgress() {
        return status == WorkoutStatus.IN_PROGRESS;
    }

    public Optional<WorkoutSet> findSet(UUID setId) {
        return sets.stream().filter(set -> set.id().equals(setId)).findFirst();
    }

    public List<WorkoutSet> setsFor(UUID exerciseId) {
        return sets.stream().filter(set -> set.exerciseId().equals(exerciseId)).toList();
    }

    public int workingSetCount() {
        return (int) sets.stream().filter(set -> set.type() == SetType.WORKING).count();
    }

    public Kg totalVolume() {
        return sets.stream()
                .filter(set -> set.type() == SetType.WORKING)
                .map(set -> set.totalLoad().times(BigDecimal.valueOf(set.reps().value())))
                .reduce(Kg.ZERO, Kg::plus);
    }
}
