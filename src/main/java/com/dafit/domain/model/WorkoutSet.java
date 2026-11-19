package com.dafit.domain.model;

import com.dafit.domain.model.LoadMode;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.WeightUnit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una serie registrada (§6 - {@code Set}).
 *
 * <p>La semantica de la carga vive aqui y en ningun otro sitio. Es el punto
 * donde se cometen los errores mas caros del dominio: confundir peso por mano
 * con peso total infla el e1RM al doble.
 *
 * <p><b>§7.1:</b> "adaptar la entrada a la definicion de la fuente, nunca
 * duplicar por defecto". Por eso el numero de implementos es explicito.
 */
public record WorkoutSet(
        UUID id,
        UUID workoutId,
        UUID exerciseId,
        int order,
        Kg load,
        WeightUnit inputUnit,
        LoadMode loadMode,
        int implementCount,
        Reps reps,
        Rir rir,
        SetType type,
        String notes,
        Instant performedAt,
        Kg bodyweightKg,
        BigDecimal bodyweightFactor
) {

    public WorkoutSet {
        Objects.requireNonNull(id, "el id de la serie es obligatorio (idempotencia C-17)");
        Objects.requireNonNull(workoutId, "la sesion es obligatoria");
        Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
        Objects.requireNonNull(loadMode, "el modo de carga es obligatorio");
        Objects.requireNonNull(reps, "las repeticiones son obligatorias");
        Objects.requireNonNull(rir, "el RIR es obligatorio");
        Objects.requireNonNull(performedAt, "la fecha de ejecucion es obligatoria");

        load = load == null ? Kg.ZERO : load;
        inputUnit = inputUnit == null ? WeightUnit.KG : inputUnit;
        type = type == null ? SetType.WORKING : type;
        implementCount = implementCount <= 0 ? 1 : implementCount;
        bodyweightFactor = bodyweightFactor == null ? BigDecimal.ONE : bodyweightFactor;

        if (!loadMode.isBodyweightBased() && !load.isPositive()) {
            throw new DomainException.Validation(
                    "una serie con carga externa necesita un peso positivo");
        }
        if (loadMode.isBodyweightBased() && (bodyweightKg == null || !bodyweightKg.isPositive())) {
            throw new DomainException.Validation(
                    "una serie de peso corporal necesita el peso corporal del dia (C-05)");
        }
    }

    /**
     * Carga total movilizada, resuelta segun el modo de carga.
     *
     * <ul>
     *   <li>TOTAL y pila de maquina: el valor ya es el total.</li>
     *   <li>PER_HAND: se multiplica por el numero de implementos (2 mancuernas).</li>
     *   <li>PER_SIDE: se multiplica por 2 (unilateral por lado).</li>
     *   <li>BODYWEIGHT: peso corporal por el factor del ejercicio (C-08).</li>
     *   <li>BODYWEIGHT_ADDED: peso corporal mas lastre.</li>
     *   <li>ASSISTED: peso corporal menos asistencia; nunca puede ser negativo.</li>
     *   <li>BAND: no cuantificable de forma fiable, se trata como cero.</li>
     * </ul>
     */
    public Kg totalLoad() {
        return switch (loadMode) {
            case TOTAL, MACHINE_STACK -> load;
            case PER_HAND -> load.times(implementCount);
            case PER_SIDE -> load.times(2);
            case BODYWEIGHT -> bodyweightKg.times(bodyweightFactor);
            case BODYWEIGHT_ADDED -> bodyweightKg.times(bodyweightFactor).plus(load);
            case ASSISTED -> {
                Kg assisted = bodyweightKg.times(bodyweightFactor).minus(load);
                if (!assisted.isPositive()) {
                    throw new DomainException.Validation(
                            "la asistencia no puede igualar o superar el peso corporal movilizado");
                }
                yield assisted;
            }
            case BAND -> Kg.ZERO;
        };
    }

    /** §7.1 - condiciones para que la serie pueda entrar en el calculo oficial. */
    public boolean isEligibleForOfficialEstimation() {
        return type == SetType.WORKING
                && reps.isOfficialRange()
                && rir.isOfficialRange()
                && totalLoad().isPositive();
    }

    public boolean isHighConfidence() {
        return isEligibleForOfficialEstimation() && reps.isHighConfidence();
    }

    public WorkoutSet withOrder(int newOrder) {
        return new WorkoutSet(id, workoutId, exerciseId, newOrder, load, inputUnit, loadMode,
                implementCount, reps, rir, type, notes, performedAt, bodyweightKg, bodyweightFactor);
    }
}
