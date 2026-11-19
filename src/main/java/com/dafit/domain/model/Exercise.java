package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Ejercicio del catalogo (§6).
 *
 * <p>Las variantes se modelan como ejercicios distintos (barra, mancuernas,
 * maquina, polea, peso corporal), tal como exige §4.
 *
 * @param bodyweightFactor fraccion del peso corporal que se moviliza en modos
 *                         de peso corporal (C-08). 1,00 en dominada; valores
 *                         menores en variantes asistidas por el suelo.
 *                         <b>PENDIENTE</b>: validar por ejercicio.
 */
public record Exercise(
        UUID id,
        String name,
        Equipment equipment,
        LoadMode loadMode,
        boolean unilateral,
        Set<MuscleGroup> muscles,
        boolean evaluable,
        BigDecimal bodyweightFactor,
        boolean active
) {

    public static final BigDecimal DEFAULT_BODYWEIGHT_FACTOR = BigDecimal.ONE;

    public Exercise {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(loadMode, "el modo de carga es obligatorio");
        if (name == null || name.isBlank()) {
            throw new DomainException.Validation("el nombre del ejercicio es obligatorio");
        }
        if (muscles == null || muscles.isEmpty()) {
            throw new DomainException.Validation("todo ejercicio debe tener al menos un grupo muscular");
        }
        muscles = Set.copyOf(muscles);
        equipment = equipment == null ? Equipment.OTHER : equipment;
        if (loadMode.isBodyweightBased() && bodyweightFactor == null) {
            bodyweightFactor = DEFAULT_BODYWEIGHT_FACTOR;
        }
        if (bodyweightFactor != null
                && (bodyweightFactor.signum() < 0 || bodyweightFactor.compareTo(BigDecimal.ONE) > 0)) {
            throw new DomainException.Validation(
                    "el factor de peso corporal debe estar entre 0 y 1: " + bodyweightFactor);
        }
    }

    public boolean involves(MuscleGroup muscle) {
        return muscles.contains(muscle);
    }

    /** Solo los ejercicios marcados como evaluables pueden generar rangos. */
    public boolean isRankable() {
        return evaluable && active;
    }
}
