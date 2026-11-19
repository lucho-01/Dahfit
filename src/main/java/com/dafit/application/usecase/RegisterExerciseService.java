package com.dafit.application.usecase;

import com.dafit.domain.port.in.RegisterExerciseUseCase;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.domain.model.Exercise;

import java.util.UUID;

/** Caso de uso: alta de ejercicio. */
public class RegisterExerciseService implements RegisterExerciseUseCase {

    private final ExerciseRepository exercises;

    public RegisterExerciseService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Override
    public UUID register(Command command) {
        Exercise exercise = new Exercise(
                UUID.randomUUID(),
                command.name(),
                command.equipment(),
                command.loadMode(),
                command.unilateral(),
                command.muscles(),
                command.evaluable(),
                command.bodyweightFactor(),
                true);

        return exercises.save(exercise).id();
    }
}
