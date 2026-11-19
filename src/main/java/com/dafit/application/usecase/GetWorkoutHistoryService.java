package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Workout;
import com.dafit.domain.port.in.GetWorkoutHistoryUseCase;
import com.dafit.domain.port.out.WorkoutRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Caso de uso: consultar historial. */
public class GetWorkoutHistoryService implements GetWorkoutHistoryUseCase {

    private final WorkoutRepository workouts;
    private final AuthorizationSupport authorization;

    public GetWorkoutHistoryService(WorkoutRepository workouts, AuthorizationSupport authorization) {
        this.workouts = workouts;
        this.authorization = authorization;
    }

    @Override
    public List<Workout> between(UUID actorId, UUID userId, LocalDate from, LocalDate to) {
        authorization.checkSameUser(actorId, userId);
        return workouts.findCompletedBetween(userId, from, to);
    }

    @Override
    public List<Workout> recent(UUID actorId, UUID userId, int limit) {
        authorization.checkSameUser(actorId, userId);
        if (limit <= 0) {
            return List.of();
        }
        return workouts.findRecent(userId, limit);
    }

    @Override
    public Workout byId(UUID actorId, UUID workoutId) {
        Workout workout = workouts.findById(workoutId)
                .orElseThrow(() -> new DomainException.NotFound("sesion no encontrada: " + workoutId));
        authorization.checkSameUser(actorId, workout.userId());
        return workout;
    }

    @Override
    public Optional<Workout> inProgress(UUID actorId, UUID userId) {
        authorization.checkSameUser(actorId, userId);
        return workouts.findInProgress(userId);
    }
}
