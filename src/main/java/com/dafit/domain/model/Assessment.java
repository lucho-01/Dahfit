package com.dafit.domain.model;

import com.dafit.domain.model.Points;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Resultado de una evaluacion de fuerza.
 *
 * <p>Jerarquia cerrada en tres niveles: ejercicio, musculo y global. Los tres
 * comparten la misma forma de acceso para que la interfaz pueda renderizarlos
 * sin condicionales por tipo.
 *
 * <p>Los tres distinguen explicitamente entre "no evaluado" y "evaluado con 0
 * puntos": {@code points()} vacio nunca significa cero (§7.4).
 */
public sealed interface Assessment
        permits ExerciseAssessment, MuscleAssessment, GlobalAssessment {

    UUID userId();

    AssessmentStatus status();

    Optional<Points> points();

    Optional<StrengthRank> rank();

    Instant assessedAt();

    default boolean isOfficial() {
        return status().isOfficial();
    }

    default boolean hasData() {
        return points().isPresent();
    }
}
