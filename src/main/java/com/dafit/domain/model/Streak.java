package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Racha de dias consecutivos con actividad (C-19).
 *
 * <p>Regla sin castigo: perder la racha solo reinicia el contador actual; el
 * record historico ({@code longestDays}) se conserva y no se resta XP.
 *
 * <p>Los eventos atrasados de sincronizacion no reescriben el pasado: si llega
 * una fecha anterior o igual a la ultima actividad, la racha no cambia.
 */
public record Streak(
        UUID userId,
        int currentDays,
        int longestDays,
        LocalDate lastActivityDate
) {

    public Streak {
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        if (currentDays < 0 || longestDays < 0) {
            throw new DomainException.Validation("los dias de racha no pueden ser negativos");
        }
        if (longestDays < currentDays) {
            throw new DomainException.Validation("el record no puede ser menor que la racha actual");
        }
    }

    public static Streak empty(UUID userId) {
        return new Streak(userId, 0, 0, null);
    }

    /** Registra actividad en {@code date}. Idempotente para la misma fecha. */
    public Streak register(LocalDate date) {
        Objects.requireNonNull(date, "la fecha es obligatoria");

        if (lastActivityDate == null) {
            return new Streak(userId, 1, Math.max(1, longestDays), date);
        }
        if (!date.isAfter(lastActivityDate)) {
            return this;
        }
        int updated = date.equals(lastActivityDate.plusDays(1)) ? currentDays + 1 : 1;
        return new Streak(userId, updated, Math.max(longestDays, updated), date);
    }
}
