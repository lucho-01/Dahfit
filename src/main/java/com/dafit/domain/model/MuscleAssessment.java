package com.dafit.domain.model;

import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Points;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Evaluacion de un grupo muscular (§7.4).
 *
 * <p>Regla dura: una sola prueba secundaria no da rango oficial, y la cobertura
 * debe alcanzar el minimo configurado (C-14).
 *
 * @param coverage suma de coeficientes de los ejercicios evaluados
 * @param exercisesEvaluated ejercicios que aportaron al calculo
 * @param primaryTestsEvaluated numero de pruebas primarias validas incluidas
 */
public record MuscleAssessment(
        UUID id,
        UUID userId,
        MuscleGroup muscle,
        AssessmentStatus status,
        Optional<Points> points,
        Optional<StrengthRank> rank,
        BigDecimal coverage,
        List<UUID> exercisesEvaluated,
        int primaryTestsEvaluated,
        Instant assessedAt
) implements Assessment {

    public MuscleAssessment {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(muscle, "el musculo es obligatorio");
        Objects.requireNonNull(status, "el estado es obligatorio");
        Objects.requireNonNull(assessedAt, "la fecha de evaluacion es obligatoria");
        points = points == null ? Optional.empty() : points;
        rank = rank == null ? Optional.empty() : rank;
        coverage = coverage == null ? BigDecimal.ZERO : coverage;
        exercisesEvaluated = exercisesEvaluated == null ? List.of() : List.copyOf(exercisesEvaluated);
        if (status == AssessmentStatus.OK && points.isEmpty()) {
            throw new IllegalStateException("una evaluacion muscular OK debe tener puntos");
        }
    }

    public static MuscleAssessment of(
            UUID userId,
            MuscleGroup muscle,
            AssessmentStatus status,
            Points points,
            StrengthRank rank,
            BigDecimal coverage,
            List<UUID> exercisesEvaluated,
            int primaryTestsEvaluated,
            Instant assessedAt) {

        return new MuscleAssessment(
                UUID.randomUUID(), userId, muscle, status,
                Optional.ofNullable(points), Optional.ofNullable(rank),
                coverage, exercisesEvaluated, primaryTestsEvaluated, assessedAt);
    }

    public static MuscleAssessment notEvaluated(
            UUID userId,
            MuscleGroup muscle,
            AssessmentStatus status,
            BigDecimal coverage,
            List<UUID> exercisesEvaluated,
            int primaryTestsEvaluated,
            Instant assessedAt) {

        return new MuscleAssessment(
                UUID.randomUUID(), userId, muscle, status,
                Optional.empty(), Optional.empty(),
                coverage, exercisesEvaluated, primaryTestsEvaluated, assessedAt);
    }
}
