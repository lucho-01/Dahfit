package com.dafit.domain.port.in;

import com.dafit.domain.model.GlobalAssessment;

import java.time.Instant;
import java.util.UUID;

/**
 * Puerto de entrada: evaluar el rango general (§7.2, §7.4).
 *
 * <p>Sin los 8 musculos evaluados devuelve cobertura parcial, nunca un promedio
 * presentado como oficial.
 */
public interface EvaluateGlobalUseCase {

    GlobalAssessment evaluate(UUID actorId, UUID userId, Instant assessedAt);
}
