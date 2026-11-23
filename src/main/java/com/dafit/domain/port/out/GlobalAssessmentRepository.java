package com.dafit.domain.port.out;

import com.dafit.domain.model.GlobalAssessment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de evaluaciones globales. */
public interface GlobalAssessmentRepository {
    Optional<GlobalAssessment> findById(UUID id);
    GlobalAssessment save(GlobalAssessment assessment);
    boolean existsById(UUID id);
    List<GlobalAssessment> findByUserId(UUID userId);
    Optional<GlobalAssessment> findLatestFor(UUID uuid);
}