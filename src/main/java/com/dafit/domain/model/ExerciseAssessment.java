package com.dafit.domain.model;

import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Percentile;
import com.dafit.domain.model.Points;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Evaluacion de un ejercicio: mejor serie valida, e1RM, fuerza relativa,
 * percentil, puntos y rango (§6 - {@code Assessment}).
 *
 * <p>Guarda la version de algoritmo y la tabla usada para que el resultado sea
 * reproducible en el futuro (C-13).
 */
public record ExerciseAssessment(
        UUID id,
        UUID userId,
        UUID exerciseId,
        AssessmentStatus status,
        Optional<Points> points,
        Optional<StrengthRank> rank,
        Optional<E1RM> e1rm,
        Optional<RelativeStrength> relativeStrength,
        Optional<Percentile> percentile,
        AlgorithmVersion algorithmVersion,
        UUID benchmarkTableId,
        Optional<Kg> bodyweightUsed,
        UUID bestSetId,
        Instant assessedAt
) implements Assessment {

    public ExerciseAssessment {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
        Objects.requireNonNull(status, "el estado es obligatorio");
        Objects.requireNonNull(assessedAt, "la fecha de evaluacion es obligatoria");
        points = points == null ? Optional.empty() : points;
        rank = rank == null ? Optional.empty() : rank;
        e1rm = e1rm == null ? Optional.empty() : e1rm;
        relativeStrength = relativeStrength == null ? Optional.empty() : relativeStrength;
        percentile = percentile == null ? Optional.empty() : percentile;
        bodyweightUsed = bodyweightUsed == null ? Optional.empty() : bodyweightUsed;
        algorithmVersion = algorithmVersion == null ? AlgorithmVersion.EPLEY_V1 : algorithmVersion;
        if (status == AssessmentStatus.OK && points.isEmpty()) {
            throw new IllegalStateException("una evaluacion OK debe tener puntos");
        }
    }

    /** Evaluacion con resultado oficial. */
    public static ExerciseAssessment evaluated(
            UUID userId,
            UUID exerciseId,
            E1RM e1rm,
            Kg bodyweight,
            Percentile percentile,
            StrengthRank rank,
            AlgorithmVersion version,
            UUID benchmarkTableId,
            UUID bestSetId,
            Instant assessedAt) {

        return new ExerciseAssessment(
                UUID.randomUUID(), userId, exerciseId, AssessmentStatus.OK,
                Optional.of(percentile.toPoints()), Optional.of(rank),
                Optional.of(e1rm),
                bodyweight == null ? Optional.empty() : Optional.of(RelativeStrength.of(e1rm.value(), bodyweight)),
                Optional.of(percentile),
                version, benchmarkTableId,
                bodyweight == null ? Optional.empty() : Optional.of(bodyweight),
                bestSetId, assessedAt);
    }

    /** Evaluacion sin resultado oficial, con el motivo explicito (C-15). */
    public static ExerciseAssessment notEvaluated(
            UUID userId,
            UUID exerciseId,
            AssessmentStatus status,
            Optional<E1RM> e1rm,
            Kg bodyweight,
            AlgorithmVersion version,
            UUID bestSetId,
            Instant assessedAt) {

        if (status == AssessmentStatus.OK) {
            throw new IllegalArgumentException("usa evaluated() para un resultado oficial");
        }
        return new ExerciseAssessment(
                UUID.randomUUID(), userId, exerciseId, status,
                Optional.empty(), Optional.empty(),
                e1rm,
                e1rm.flatMap(value -> bodyweight == null
                        ? Optional.<RelativeStrength>empty()
                        : Optional.of(RelativeStrength.of(value.value(), bodyweight))),
                Optional.empty(),
                version, null,
                bodyweight == null ? Optional.empty() : Optional.of(bodyweight),
                bestSetId, assessedAt);
    }
}
