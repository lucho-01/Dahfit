package com.dafit.domain.port.out;

import com.dafit.domain.model.Exercise;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Equipment;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Puerto de salida: repositorio de ejercicios. */
public interface ExerciseRepository {
    Optional<Exercise> findById(UUID id);
    Exercise save(Exercise exercise);
    boolean existsById(UUID id);
    List<Exercise> findAll();
    List<Exercise> findAllActive();
    List<Exercise> search(Set<MuscleGroup> muscles, Equipment equipment, boolean evaluableOnly);
}