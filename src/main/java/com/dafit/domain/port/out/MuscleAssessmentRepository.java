package com.dafit.domain.port.out;

import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de evaluaciones musculares. */
public interface MuscleAssessmentRepository {
    Optional<MuscleAssessment> findById(UUID id);
    MuscleAssessment save(MuscleAssessment assessment);
    boolean existsById(UUID id);
    List<MuscleAssessment> findByUserId(UUID userId);
    List<MuscleAssessment> findByMuscleGroup(UUID userId, MuscleGroup muscle);
    Optional<MuscleAssessment> findLatestFor(UUID userId, MuscleGroup muscle);
    List<MuscleAssessment> findLatestForUser(UUID userId);
}