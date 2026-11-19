package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de XP ya otorgado (C-19).
 *
 * <p>{@code sourceKey} es la clave de idempotencia: un mismo hecho (una sesion,
 * una revision, un bonus semanal) no puede otorgar XP dos veces aunque la
 * peticion se reenvie. La base de datos impone {@code UNIQUE (user_id,
 * source_key)} para que la regla no dependa solo del codigo.
 *
 * <p>El XP es una metrica de constancia: nunca entra en el calculo de rango.
 */
public record XpEvent(
        UUID id,
        UUID userId,
        XpEventType type,
        int points,
        LocalDate localDate,
        Instant occurredAt,
        String sourceKey
) {

    public XpEvent {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(type, "el tipo de evento es obligatorio");
        Objects.requireNonNull(localDate, "la fecha local es obligatoria");
        Objects.requireNonNull(occurredAt, "el instante es obligatorio");
        if (sourceKey == null || sourceKey.isBlank()) {
            throw new DomainException.Validation("la clave de idempotencia es obligatoria");
        }
        if (points < 0) {
            throw new DomainException.Validation("el XP no puede ser negativo: " + points);
        }
    }
}
