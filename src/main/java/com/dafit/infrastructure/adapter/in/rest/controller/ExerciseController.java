package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.port.in.ListExercisesUseCase;
import com.dafit.domain.port.in.RegisterExerciseUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RegisterExerciseRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.ExerciseResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.IdResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Catalogo de ejercicios. */
@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {

    private final ListExercisesUseCase listExercises;
    private final RegisterExerciseUseCase registerExercise;

    public ExerciseController(ListExercisesUseCase listExercises, RegisterExerciseUseCase registerExercise) {
        this.listExercises = listExercises;
        this.registerExercise = registerExercise;
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
