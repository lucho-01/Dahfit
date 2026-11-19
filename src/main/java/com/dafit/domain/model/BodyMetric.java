package com.dafit.domain.model;

import com.dafit.domain.model.Kg;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Medida corporal historica (§6 - {@code BodyMetric}).
 *
 * <p>Es la fuente de verdad del peso corporal usado en la fuerza relativa.
 * Nunca se sobrescribe: cada pesada es una fila nueva.
 */
public record BodyMetric(
        UUID id,
        UUID userId,
        Kg weightKg,
        LocalDate date,
        Instant recordedAt,
        String source
) {

    public static final String SOURCE_MANUAL = "MANUAL";

    public BodyMetric {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(weightKg, "el peso es obligatorio");
        Objects.requireNonNull(date, "la fecha es obligatoria");
        if (!weightKg.isPositive()) {
            throw new IllegalArgumentException("el peso corporal debe ser positivo");
        }
        recordedAt = recordedAt == null ? Instant.now() : recordedAt;
        source = source == null || source.isBlank() ? SOURCE_MANUAL : source;
    }

    public static BodyMetric of(UUID userId, Kg weightKg, LocalDate date) {
        return new BodyMetric(UUID.randomUUID(), userId, weightKg, date, Instant.now(), SOURCE_MANUAL);
    }
}
