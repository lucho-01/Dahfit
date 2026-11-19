package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Routine;
import com.dafit.domain.port.in.GetRoutinesUseCase;
import com.dafit.domain.port.out.RoutineRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Caso de uso: consultar rutinas. */
public class GetRoutinesService implements GetRoutinesUseCase {

    private final RoutineRepository routines;
    private final AuthorizationSupport authorization;

    public GetRoutinesService(RoutineRepository routines, AuthorizationSupport authorization) {
        this.routines = routines;
        this.authorization = authorization;
    }

    @Override
    public Routine byId(UUID actorId, UUID routineId) {
        Routine routine = routines.findById(routineId)
                .orElseThrow(() -> new DomainException.NotFound("rutina no encontrada: " + routineId));
        authorization.checkAthleteAccess(actorId, routine.ownerId());
        return routine;
    }

    @Override
    public List<Routine> forUser(UUID actorId, UUID userId) {
        authorization.checkAthleteAccess(actorId, userId);

        // Una rutina puede estar poseida y asignada al mismo usuario (un coach
        // que entrena su propia rutina). Se deduplica por id antes de ordenar.
        Map<UUID, Routine> byId = new LinkedHashMap<>();
        routines.findOwnedBy(userId).forEach(routine -> byId.putIfAbsent(routine.id(), routine));
        routines.findAssignedTo(userId).forEach(routine -> byId.putIfAbsent(routine.id(), routine));

        return byId.values().stream()
                .sorted(Comparator.comparing(Routine::createdAt).reversed())
                .toList();
    }
}
