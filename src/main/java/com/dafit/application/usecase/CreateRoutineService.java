package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Routine;
import com.dafit.domain.port.in.CreateRoutineUseCase;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.domain.port.out.RoutineRepository;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.domain.model.RoutineExercise;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: crear, duplicar y asignar rutinas.
 *
 * <ul>
 *   <li><b>R-02:</b> crear una rutina para OTRO usuario exige ser coach o
 *       ADMIN; un atleta solo crea rutinas propias.</li>
 *   <li><b>R-03:</b> duplicar y asignar exigen ser el propietario.</li>
 *   <li><b>R-07:</b> todos los ejercicios de la rutina deben existir.</li>
 * </ul>
 */
public class CreateRoutineService implements CreateRoutineUseCase {

    private final RoutineRepository routines;
    private final ExerciseRepository exercises;
    private final UserRepository users;
    private final AuthorizationSupport authorization;

    public CreateRoutineService(RoutineRepository routines,
                                ExerciseRepository exercises,
                                UserRepository users,
                                AuthorizationSupport authorization) {
        this.routines = routines;
        this.exercises = exercises;
        this.users = users;
        this.authorization = authorization;
    }

    @Override
    public UUID create(Command command) {
        boolean forSelf = command.actorId() == null || command.actorId().equals(command.ownerId());
        if (!forSelf) {
            authorization.checkAthleteAccess(command.actorId(), command.ownerId());
            if (!authorization.isCoach(command.actorId()) && !authorization.isAdmin(command.actorId())) {
                throw new DomainException.Forbidden(
                        "un atleta solo puede crear rutinas propias");
            }
        }
        requireUser(command.ownerId());
        requireExercises(command.exercises());

        List<RoutineExercise> ordered = new ArrayList<>();
        int order = 1;
        for (ExerciseSpec spec : command.exercises()) {
            ordered.add(new RoutineExercise(
                    spec.exerciseId(), order++, spec.targetSets(), spec.targetReps(), spec.restSeconds()));
        }
        Routine routine = Routine.create(command.ownerId(), command.name(), ordered, Instant.now());
        return routines.save(routine).id();
    }

    @Override
    public UUID duplicate(Command command) {
        Routine original = routines.findById(command.routineId())
                .orElseThrow(() -> new DomainException.NotFound("rutina no encontrada"));
        authorization.checkOwner(command.actorId(), original.ownerId());
        requireUser(command.newOwnerId());

        return routines.save(original.duplicateFor(command.newOwnerId(), command.newName())).id();
    }

    @Override
    public UUID assign(Command command) {
        Routine routine = routines.findById(command.routineId())
                .orElseThrow(() -> new DomainException.NotFound("rutina no encontrada"));
        authorization.checkOwner(command.actorId(), routine.ownerId());
        requireUser(command.athleteId());

        return routines.save(routine.assignTo(command.athleteId())).id();
    }

    private void requireUser(UUID userId) {
        if (users.findById(userId).isEmpty()) {
            throw new DomainException.NotFound("usuario no encontrado: " + userId);
        }
    }

    private void requireExercises(List<ExerciseSpec> specs) {
        Set<UUID> seen = new HashSet<>();
        for (ExerciseSpec spec : specs) {
            if (exercises.findById(spec.exerciseId()).isEmpty()) {
                throw new DomainException.NotFound(
                        "ejercicio no encontrado: " + spec.exerciseId());
            }
            if (!seen.add(spec.exerciseId())) {
                throw new DomainException.Validation(
                        "el ejercicio " + spec.exerciseId() + " aparece repetido en la rutina");
            }
        }
    }
}
