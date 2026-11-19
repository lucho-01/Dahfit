package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Operacion offline pendiente de subir (C-17).
 *
 * <p>El cliente la genera sin conexion y la reenvia hasta recibir confirmacion.
 * Por eso cada item lleva una clave estable ({@link #key()}): el servidor puede
 * distinguir "aplicado" de "ya lo tenia" sin confiar en el orden ni en que el
 * dispositivo recuerde lo que envio.
 */
public sealed interface SyncItem permits SyncItem.WorkoutSetItem, SyncItem.WorkoutFinishItem {

    UUID workoutId();

    SyncTarget target();

    /** Clave estable de idempotencia dentro del lote y entre reintentos. */
    String key();

    /** Serie registrada en el dispositivo. */
    record WorkoutSetItem(
            UUID clientUuid,
            UUID workoutId,
            UUID exerciseId,
            Kg load,
            WeightUnit inputUnit,
            LoadMode loadMode,
            int implementCount,
            Reps reps,
            Rir rir,
            SetType type,
            String notes,
            Instant performedAt
    ) implements SyncItem {

        public WorkoutSetItem {
            Objects.requireNonNull(clientUuid, "el clientUuid es obligatorio");
            Objects.requireNonNull(workoutId, "la sesion es obligatoria");
            Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
            Objects.requireNonNull(reps, "las repeticiones son obligatorias");
            // R-06: load y rir pueden faltar en un item offline mal formado.
            // No se inventan aqui: el caso de uso rechaza ESE item y sigue con
            // el resto del lote. Antes un RIR ausente se guardaba como 0 y una
            // carga ausente tumbaba el lote entero con un 500.
            if (implementCount < 1) {
                throw new DomainException.Validation("implementCount debe ser >= 1");
            }
        }

        @Override
        public SyncTarget target() {
            return SyncTarget.WORKOUT_SET;
        }

        @Override
        public String key() {
            return clientUuid.toString();
        }
    }

    /** Cierre de la sesion desde el dispositivo. */
    record WorkoutFinishItem(UUID workoutId, Instant finishedAt) implements SyncItem {

        public WorkoutFinishItem {
            Objects.requireNonNull(workoutId, "la sesion es obligatoria");
        }

        @Override
        public SyncTarget target() {
            return SyncTarget.WORKOUT_FINISH;
        }

        @Override
        public String key() {
            return workoutId + ":finish";
        }
    }
}
