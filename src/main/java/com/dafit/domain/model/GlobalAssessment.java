package com.dafit.domain.model;

import com.dafit.domain.model.Points;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Rango general (§7.2, §7.4).
 *
 * <p>Regla dura: el rango global oficial exige los 8 musculos evaluados. Un
 * promedio de 7 NUNCA se presenta como oficial; se expone como cobertura.
 *
 * @param musclesEvaluated musculos con evaluacion oficial
 * @param musclesRequired total exigido (8)
 */
public record GlobalAssessment(
        UUID id,
        UUID userId,
        AssessmentStatus status,
        Optional<Points> points,
        Optional<StrengthRank> rank,
        int musclesEvaluated,
        int musclesRequired,
        Instant assessedAt
) implements Assessment {

    public GlobalAssessment {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(status, "el estado es obligatorio");
        Objects.requireNonNull(assessedAt, "la fecha de evaluacion es obligatoria");
        points = points == null ? Optional.empty() : points;
        rank = rank == null ? Optional.empty() : rank;
        if (musclesRequired <= 0) {
            throw new IllegalArgumentException("musclesRequired debe ser positivo");
        }
        if (status == AssessmentStatus.OK && musclesEvaluated == 0) {
            throw new IllegalStateException("un rango global OK exige al menos un musculo evaluado");
        }
        if (musclesEvaluated > musclesRequired) {
            throw new IllegalStateException("musculos evaluados (" + musclesEvaluated
                    + ") no puede superar el total exigido (" + musclesRequired + ")");
        }
    }

    public static GlobalAssessment official(
            UUID userId, Points points, StrengthRank rank, int musclesRequired, Instant assessedAt) {

        return new GlobalAssessment(
                UUID.randomUUID(), userId, AssessmentStatus.OK,
                Optional.of(points), Optional.of(rank),
                musclesRequired, musclesRequired, assessedAt);
    }

    public static GlobalAssessment incomplete(
            UUID userId, int musclesEvaluated, int musclesRequired, Instant assessedAt) {

        return new GlobalAssessment(
                UUID.randomUUID(), userId, AssessmentStatus.COBERTURA_INSUFICIENTE,
                Optional.empty(), Optional.empty(),
                musclesEvaluated, musclesRequired, assessedAt);
    }

    /** Progreso de cobertura 0-100 para la interfaz, nunca un rango. */
    public int coveragePercent() {
        return (int) Math.round(100.0 * musclesEvaluated / musclesRequired);
    }
}
