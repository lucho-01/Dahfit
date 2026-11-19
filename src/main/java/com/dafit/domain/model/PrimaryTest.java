package com.dafit.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Prueba primaria (o sustitucion permitida) de un musculo (§4, §7.4).
 *
 * <p>Sin al menos una prueba primaria valida, el musculo queda "Sin evaluar".
 * Este agregado es el que permite aplicar esa regla: sin el, la regla existia en
 * el documento pero no en el modelo (ver §2.4 del analisis).
 */
public record PrimaryTest(
        MuscleGroup muscle,
        UUID exerciseId,
        boolean primary
) {

    public PrimaryTest {
        Objects.requireNonNull(muscle, "el musculo es obligatorio");
        Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
    }

    public static PrimaryTest primary(MuscleGroup muscle, UUID exerciseId) {
        return new PrimaryTest(muscle, exerciseId, true);
    }

    public static PrimaryTest substitution(MuscleGroup muscle, UUID exerciseId) {
        return new PrimaryTest(muscle, exerciseId, false);
    }
}
