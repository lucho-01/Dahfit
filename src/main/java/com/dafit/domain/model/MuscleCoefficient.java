package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Coeficiente de participacion de un ejercicio en un musculo (§7.4).
 *
 * <p>Son ponderaciones de diseno, no porcentajes fisiologicos medidos. Se
 * mantienen versionadas y editables por administrador.
 *
 * <p><b>Nota de implementacion (C-09):</b> el coeficiente NO escala la
 * puntuacion del musculo -eso es lo que provocaba que registrar un ejercicio
 * debil bajara la nota-. Aqui el coeficiente sirve para dos cosas: decidir que
 * ejercicios cuentan para el musculo y medir la cobertura alcanzada (C-14).
 */
public record MuscleCoefficient(
        UUID exerciseId,
        MuscleGroup muscle,
        BigDecimal coefficient,
        String version,
        boolean approved
) {

    public MuscleCoefficient {
        Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
        Objects.requireNonNull(muscle, "el musculo es obligatorio");
        Objects.requireNonNull(coefficient, "el coeficiente es obligatorio");
        if (coefficient.signum() < 0 || coefficient.compareTo(BigDecimal.ONE) > 0) {
            throw new DomainException.Validation(
                    "el coeficiente debe estar entre 0 y 1: " + coefficient);
        }
        version = version == null || version.isBlank() ? "v1" : version;
    }

    public boolean isEffective() {
        return approved && coefficient.signum() > 0;
    }
}
