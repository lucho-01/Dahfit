package com.dafit.domain.service;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Points;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Agregacion de la especificacion original (§7.2):
 * {@code sum(puntos x coeficiente) / sum(coeficientes)}.
 *
 * <p><b>Se conserva solo como referencia historica.</b> No usar en produccion:
 * es una media ponderada y, por tanto, registrar un ejercicio flojo reduce la
 * puntuacion del musculo (C-09). Se deja implementada para poder comparar
 * resultados entre ambas politicas durante la decision.
 */
public class WeightedMeanAggregation implements MuscleAggregationStrategy {

    public static final String ID = "WEIGHTED_MEAN_SPEC";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<Points> aggregate(List<MuscleContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal coefficientSum = BigDecimal.ZERO;

        for (MuscleContribution contribution : contributions) {
            BigDecimal coefficient = contribution.coefficient();
            weightedSum = weightedSum.add(
                    BigDecimal.valueOf(contribution.points().value()).multiply(coefficient));
            coefficientSum = coefficientSum.add(coefficient);
        }

        if (coefficientSum.signum() == 0) {
            throw new DomainException.Validation("la suma de coeficientes no puede ser cero");
        }

        BigDecimal mean = weightedSum.divide(coefficientSum, MathContext.DECIMAL64)
                .setScale(0, RoundingMode.HALF_UP);

        return Optional.of(Points.of(mean.intValueExact()));
    }
}
