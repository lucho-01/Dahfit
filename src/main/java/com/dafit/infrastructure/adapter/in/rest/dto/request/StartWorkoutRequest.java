package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Apertura de una sesion de entrenamiento.
 *
 * <p>{@code localDate} es opcional: si falta se usa "hoy" en la zona del
 * usuario. {@code bodyweightKg} permite fijar el peso del dia sin esperar a una
 * pesada; si falta se usa el del perfil (C-05).
 */
public record StartWorkoutRequest(
        @NotNull UUID userId,
        LocalDate localDate,
        String timeZone,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal bodyweightKg
) {
}
