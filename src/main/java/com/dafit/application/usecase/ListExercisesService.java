package com.dafit.application.usecase;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Exercise;
import com.dafit.domain.port.in.ListExercisesUseCase;
import com.dafit.domain.port.out.ExerciseRepository;

import java.util.List;
import java.util.UUID;

/** Caso de uso: consultar catalogo. */
public class ListExercisesService implements ListExercisesUseCase {

    private final ExerciseRepository exercises;

    public ListExercisesService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Override
    public List<Exercise> list(Filter filter) {
        if (filter == null) {
            return exercises.findAllActive();
        }
        return exercises.search(filter.muscles(), filter.equipment(), filter.onlyEvaluable());
    }

    @Override
    public Exercise byId(UUID exerciseId) {
        return exercises.findById(exerciseId)
                .orElseThrow(() -> new DomainException.NotFound("ejercicio no encontrado: " + exerciseId));
    }
}
