package com.dafit.domain.port.in;

import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.MuscleAssessment;

import java.time.Instant;
import java.util.UUID;

/**
 * Puerto de entrada: evaluar un grupo muscular (§7.4).
 *
 * <p>La cobertura y la exigencia de prueba primaria se aplican dentro; el
 * llamador no puede "forzar" un rango oficial.
 */
public interface EvaluateMuscleUseCase {

    /**
     * @param actorId usuario autenticado ({@code null} con el API abierto)
     * @throws com.dafit.domain.exception.DomainException.Forbidden si el actor no puede operar sobre el usuario
     */
    MuscleAssessment evaluate(UUID actorId, UUID userId, MuscleGroup muscle, Instant assessedAt);
}
