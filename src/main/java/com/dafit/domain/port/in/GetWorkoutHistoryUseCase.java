package com.dafit.domain.port.in;

import com.dafit.domain.model.Workout;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: historial de entrenamiento (§5). */
public interface GetWorkoutHistoryUseCase {

    List<Workout> between(UUID actorId, UUID userId, LocalDate from, LocalDate to);

    List<Workout> recent(UUID actorId, UUID userId, int limit);

    /**
     * @throws com.dafit.domain.exception.DomainException.NotFound si no existe
     */
    Workout byId(UUID actorId, UUID workoutId);

    /** Sesion abierta del usuario, para retomarla al reabrir la app. */
    Optional<Workout> inProgress(UUID actorId, UUID userId);
}
