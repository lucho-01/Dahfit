package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.port.in.CreateRoutineUseCase;
import com.dafit.domain.port.in.GetRoutinesUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.CreateRoutineRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.IdResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.RoutineResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Creacion, duplicacion, asignacion y consulta de rutinas. */
@RestController
@RequestMapping("/api")
public class RoutineController {

    private final CreateRoutineUseCase createRoutine;
    private final GetRoutinesUseCase getRoutines;

    public RoutineController(CreateRoutineUseCase createRoutine, GetRoutinesUseCase getRoutines) {
        this.createRoutine = createRoutine;
        this.getRoutines = getRoutines;
    }

    @PostMapping("/routines")
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse create(Authentication authentication,
                             @Valid @RequestBody CreateRoutineRequest request) {
        return IdResponse.of(createRoutine.create(
                RequestMapper.toCommand(Actors.actorId(authentication), request)));
    }

    @GetMapping("/routines/{routineId}")
    public RoutineResponse byId(Authentication authentication, @PathVariable UUID routineId) {
        return ResponseMapper.routine(getRoutines.byId(Actors.actorId(authentication), routineId));
    }

    /** Rutinas que el usuario posee o tiene asignadas. */
    @GetMapping("/users/{userId}/routines")
    public List<RoutineResponse> forUser(Authentication authentication,
                                         @PathVariable UUID userId) {
        return getRoutines.forUser(Actors.actorId(authentication), userId).stream()
                .map(ResponseMapper::routine)
                .toList();
    }

    @PostMapping("/routines/{routineId}/duplicate")
    public IdResponse duplicate(Authentication authentication,
                                @PathVariable UUID routineId,
                                @RequestParam UUID newOwnerId,
                                @RequestParam String newName) {
        return IdResponse.of(createRoutine.duplicate(RequestMapper.toDuplicateCommand(
                Actors.actorId(authentication), routineId, newOwnerId, newName)));
    }

    @PostMapping("/routines/{routineId}/assign")
    public IdResponse assign(Authentication authentication,
                             @PathVariable UUID routineId,
                             @RequestParam UUID athleteId) {
        return IdResponse.of(createRoutine.assign(RequestMapper.toAssignCommand(
                Actors.actorId(authentication), routineId, athleteId)));
    }
}
