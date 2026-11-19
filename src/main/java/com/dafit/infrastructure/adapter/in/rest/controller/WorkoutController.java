package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.model.Workout;
import com.dafit.domain.port.in.AwardWorkoutXpUseCase;
import com.dafit.domain.port.in.GetWorkoutHistoryUseCase;
import com.dafit.domain.port.in.WorkoutSessionUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.LogSetRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.StartWorkoutRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.IdResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.WorkoutResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Ciclo de vida de una sesion y consulta de historial. */
@RestController
@RequestMapping("/api")
public class WorkoutController {

    private static final int DEFAULT_HISTORY_LIMIT = 50;

    private final WorkoutSessionUseCase session;
    private final GetWorkoutHistoryUseCase history;
    private final AwardWorkoutXpUseCase awardXp;

    public WorkoutController(WorkoutSessionUseCase session,
                             GetWorkoutHistoryUseCase history,
                             AwardWorkoutXpUseCase awardXp) {
        this.session = session;
        this.history = history;
        this.awardXp = awardXp;
    }

    @PostMapping("/workouts")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse start(Authentication authentication,
                            @Valid @RequestBody StartWorkoutRequest request) {
        return IdResponse.of(session.start(
                RequestMapper.toCommand(Actors.actorId(authentication), request)));
    }

    @PostMapping("/workouts/{workoutId}/sets")
    public WorkoutResponse logSet(Authentication authentication,
                                  @PathVariable UUID workoutId,
                                  @Valid @RequestBody LogSetRequest request) {
        return ResponseMapper.workout(session.logSet(
                RequestMapper.toCommand(Actors.actorId(authentication), workoutId, request)));
    }

    @PostMapping("/workouts/{workoutId}/finish")
    public WorkoutResponse finish(Authentication authentication, @PathVariable UUID workoutId) {
        Workout finished = session.finish(new WorkoutSessionUseCase.FinishCommand(
                Actors.actorId(authentication), workoutId));
        // El XP se otorga despues de persistir la sesion y es idempotente por
        // workoutId: cerrar de nuevo no vuelve a premiar.
        awardXp.onWorkoutCompleted(finished, finished.finishedAt());
        return ResponseMapper.workout(finished);
    }

    @GetMapping("/workouts/{workoutId}")
    public WorkoutResponse byId(Authentication authentication, @PathVariable UUID workoutId) {
        return ResponseMapper.workout(history.byId(Actors.actorId(authentication), workoutId));
    }

    @GetMapping("/users/{userId}/workouts")
    public List<WorkoutResponse> history(
            Authentication authentication,
            @PathVariable UUID userId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer limit) {

        UUID actorId = Actors.actorId(authentication);
        List<Workout> workouts;
        if (from != null && to != null) {
            workouts = history.between(actorId, userId, from, to);
        } else {
            workouts = history.recent(actorId, userId, limit == null ? DEFAULT_HISTORY_LIMIT : limit);
        }
        return workouts.stream().map(ResponseMapper::workout).toList();
    }

    /**
     * Sesion abierta del usuario, para retomarla al reabrir la app.
     *
     * <p>204 si no hay ninguna en curso: es el estado normal, no un error.
     */
    @GetMapping("/users/{userId}/workouts/current")
    public ResponseEntity<WorkoutResponse> current(Authentication authentication,
                                                   @PathVariable UUID userId) {
        return history.inProgress(Actors.actorId(authentication), userId)
                .map(ResponseMapper::workout)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
