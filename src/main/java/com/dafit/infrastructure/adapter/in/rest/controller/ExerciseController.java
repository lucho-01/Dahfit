package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.port.in.ListExercisesUseCase;
import com.dafit.domain.port.in.RegisterExerciseUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.response.IdResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.ExerciseResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RegisterExerciseRequest;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import com.dafit.infrastructure.config.DafitProperties;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ListExercisesUseCase listExercises;
    private final RegisterExerciseUseCase registerExercise;
    private final AuthorizationSupport authorization;
    private final DafitProperties properties;

    public ExerciseController(ListExercisesUseCase listExercises,
                              RegisterExerciseUseCase registerExercise,
                              AuthorizationSupport authorization,
                              DafitProperties properties) {
        this.listExercises = listExercises;
        this.registerExercise = registerExercise;
        this.authorization = authorization;
        this.properties = properties;
    }

    @GetMapping
    public List<ExerciseResponse> list(
            @RequestParam(required = false) Set<MuscleGroup> muscles,
            @RequestParam(required = false) Equipment equipment,
            @RequestParam(defaultValue = "false") boolean evaluable) {

        var filter = new ListExercisesUseCase.Filter(
                muscles == null ? Set.of() : muscles, equipment, evaluable);

        return listExercises.list(filter).stream()
                .map(ResponseMapper::exercise)
                .toList();
    }

    @GetMapping("/{exerciseId}")
    public ExerciseResponse byId(@PathVariable UUID exerciseId) {
        return ResponseMapper.exercise(listExercises.byId(exerciseId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdResponse register(@Valid @RequestBody RegisterExerciseRequest request) {
        return IdResponse.of(registerExercise.register(RequestMapper.toCommand(request)));
    }
}