package com.dafit.domain.port.out;

import com.dafit.domain.model.ExerciseAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de evaluaciones de ejercicios. */
public interface ExerciseAssessmentRepository {
    Optional<ExerciseAssessment> findById(UUID id);
    ExerciseAssessment save(ExerciseAssessment assessment);
    boolean existsById(UUID id);
    List<ExerciseAssessment> findByUserId(UUID userId);
    List<ExerciseAssessment> findByExerciseId(UUID exerciseId);
    Optional<ExerciseAssessment> findLatestFor(UUID userId, UUID exerciseId);
    List<ExerciseAssessment> findLatestForUser(UUID userId);
}