package com.dafit.domain.service;

import com.dafit.domain.model.Points;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Aportacion de un ejercicio al calculo de un musculo.
 *
 * @param coefficient coeficiente de participacion del ejercicio en ese musculo
 */
public record MuscleContribution(UUID exerciseId, Points points, BigDecimal coefficient) {

    public MuscleContribution {
        Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
        Objects.requireNonNull(points, "los puntos son obligatorios");
        Objects.requireNonNull(coefficient, "el coeficiente es obligatorio");
    }
}
