package com.dafit.domain.port.out;

import com.dafit.domain.model.Workout;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de sesiones de entrenamiento. */
public interface WorkoutRepository {
    Optional<Workout> findById(UUID id);
    Workout save(Workout workout);
    boolean existsById(UUID id);
    List<Workout> findByUserId(UUID userId);
    List<Workout> findRecent(UUID userId, int lookback);
    List<Workout> findCompletedByUserId(UUID userId);
    long countCompleted(UUID userId);
    List<Workout> findCompletedBetween(UUID userId, LocalDate start, LocalDate end);
    Optional<Workout> findInProgress(UUID userId);
}