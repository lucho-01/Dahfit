package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.time.LocalDate;
import java.util.Objects;

/**
 * XP propuesto por el caso de uso antes de aplicar los topes de C-19.
 *
 * <p>El {@code XpCalculator} puede reducir {@code points} (o descartar la
 * propuesta entera) si el usuario ya alcanzo el tope diario, el semanal o el
 * maximo de sesiones que dan XP en el dia.
 */
public record XpGrant(
        XpEventType type,
        int points,
        LocalDate localDate,
        String sourceKey
) {

    public XpGrant {
        Objects.requireNonNull(type, "el tipo de evento es obligatorio");
        Objects.requireNonNull(localDate, "la fecha local es obligatoria");
        if (sourceKey == null || sourceKey.isBlank()) {
            throw new DomainException.Validation("la clave de idempotencia es obligatoria");
        }
        if (points < 0) {
            throw new DomainException.Validation("el XP no puede ser negativo: " + points);
        }
    }
}
