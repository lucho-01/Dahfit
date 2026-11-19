package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Alta de una serie en una sesion abierta.
 *
 * <p>{@code clientUuid} es el identificador del dispositivo: reenviar la misma
 * serie no duplica (C-17). El dominio valida la carga segun el modo.
 */
public record LogSetRequest(
        @NotNull UUID clientUuid,
        @NotNull UUID exerciseId,
        BigDecimal loadKg,
        String inputUnit,
        @NotNull String loadMode,
        int implementCount,
        @NotNull Integer reps,
        @NotNull Integer rir,
        String type,
        String notes,
        Instant performedAt,
        BigDecimal bodyweightKg,
        BigDecimal bodyweightFactor
) {
}
