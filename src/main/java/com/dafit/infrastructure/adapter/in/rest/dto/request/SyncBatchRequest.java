package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lote de operaciones offline (C-17).
 *
 * <p>Se usa un unico DTO con campos opcionales y {@code target} explicito en
 * lugar de herencia polimorfica de Jackson: el contrato queda plano, es facil
 * de generar en el cliente y el mapper valida lo que exige cada tipo.
 */
public record SyncBatchRequest(
        @NotNull UUID userId,
        @NotEmpty List<@Valid SyncItemRequest> items
) {

    public record SyncItemRequest(
            @NotNull String target,
            UUID clientUuid,
            @NotNull UUID workoutId,
            UUID exerciseId,
            BigDecimal loadKg,
            String inputUnit,
            String loadMode,
            Integer implementCount,
            Integer reps,
            Integer rir,
            String type,
            String notes,
            Instant performedAt,
            Instant finishedAt
    ) {
    }
}
