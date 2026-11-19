package com.dafit.domain.port.in;

import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.WeightUnit;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.Workout;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Puerto de entrada: ciclo de vida de una sesion (§5, "Entrenamiento activo").
 *
 * <p>Objetivo de producto: registrar una serie en menos de 90 segundos por
 * ejercicio (§2). Por eso el caso de uso acepta la serie ya resuelta y no
 * obliga a pasos intermedios.
 *
 * <p>{@code actorId} es el usuario autenticado: solo puede abrir, anotar y
 * cerrar sus propias sesiones (R-03). Es {@code null} con el API abierto.
 */
public interface WorkoutSessionUseCase {

    UUID start(StartCommand command);

    /** @throws com.dafit.domain.exception.DomainException.NotFound si el ejercicio no existe */
    Workout logSet(SetCommand command);

    Workout finish(FinishCommand command);

    /** @param clientUuid identificador generado en el dispositivo (C-17) */
    record SetCommand(
            UUID actorId,
            UUID workoutId,
            UUID clientUuid,
            UUID exerciseId,
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
    }

    record FinishCommand(UUID actorId, UUID workoutId) {
    }

    /**
     * @param timeZone       zona que declara el cliente; se valida contra la del perfil
     * @param localDate      fecha local de la sesion; {@code null} = hoy en la zona del usuario
     * @param bodyweightKg   peso corporal del dia; {@code null} = se infiere del formulario
     */
    record StartCommand(
            UUID actorId,
            UUID userId,
            LocalDate localDate,
            ZoneId timeZone,
            Kg bodyweightKg
    ) {
    }
}
