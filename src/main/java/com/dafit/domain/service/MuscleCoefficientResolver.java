package com.dafit.domain.service;

import com.dafit.domain.model.MuscleCoefficient;
import com.dafit.domain.model.MuscleGroup;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de dominio: resuelve que coeficientes aplican a un ejercicio.
 *
 * <p>Java puro: sin Spring, sin repositorios, sin E/S. Recibe los datos ya
 * cargados y devuelve el resultado.
 */
public class MuscleCoefficientResolver {

    /** Coeficiente vigente del ejercicio para el musculo, si participa en el. */
    public Optional<BigDecimal> coefficientFor(
            Collection<MuscleCoefficient> coefficients, UUID exerciseId, MuscleGroup muscle) {

        return coefficients.stream()
                .filter(MuscleCoefficient::isEffective)
                .filter(c -> c.exerciseId().equals(exerciseId))
                .filter(c -> c.muscle() == muscle)
                .map(MuscleCoefficient::coefficient)
                .findFirst();
    }

    /** Musculos en los que participa un ejercicio, con su coeficiente. */
    public Map<MuscleGroup, BigDecimal> participationOf(
            Collection<MuscleCoefficient> coefficients, UUID exerciseId) {

        return coefficients.stream()
                .filter(MuscleCoefficient::isEffective)
                .filter(c -> c.exerciseId().equals(exerciseId))
                .collect(Collectors.toMap(
                        MuscleCoefficient::muscle,
                        MuscleCoefficient::coefficient,
                        (a, b) -> a));
    }

    /**
     * Cobertura alcanzada para un musculo (C-14): suma de los coeficientes de
     * los ejercicios evaluados. Se compara contra el minimo configurado.
     */
    public BigDecimal coverage(Collection<MuscleCoefficient> coefficients,
                               MuscleGroup muscle,
                               Collection<UUID> evaluatedExerciseIds) {

        return coefficients.stream()
                .filter(MuscleCoefficient::isEffective)
                .filter(c -> c.muscle() == muscle)
                .filter(c -> evaluatedExerciseIds.contains(c.exerciseId()))
                .map(MuscleCoefficient::coefficient)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Ejercicios que el catalogo declara como prueba primaria del musculo. */
    public List<UUID> primaryExerciseIds(
            Collection<com.dafit.domain.model.PrimaryTest> primaryTests, MuscleGroup muscle) {

        return primaryTests.stream()
                .filter(t -> t.muscle() == muscle)
                .filter(com.dafit.domain.model.PrimaryTest::primary)
                .map(com.dafit.domain.model.PrimaryTest::exerciseId)
                .toList();
    }
}
