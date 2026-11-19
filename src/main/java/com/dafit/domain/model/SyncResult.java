package com.dafit.domain.model;

import java.util.Objects;

/**
 * Resultado por item de un lote de sincronizacion.
 *
 * <p>{@code DUPLICATE} no es un error: significa que el servidor ya tenia ese
 * dato y el cliente puede borrarlo de su cola local. {@code REJECTED} si es un
 * fallo de negocio y el mensaje explica por que.
 */
public record SyncResult(
        String key,
        SyncTarget target,
        SyncOutcome outcome,
        String message
) {

    public SyncResult {
        Objects.requireNonNull(key, "la clave es obligatoria");
        Objects.requireNonNull(target, "el tipo es obligatorio");
        Objects.requireNonNull(outcome, "el resultado es obligatorio");
    }

    public static SyncResult applied(String key, SyncTarget target) {
        return new SyncResult(key, target, SyncOutcome.APPLIED, null);
    }

    public static SyncResult duplicate(String key, SyncTarget target) {
        return new SyncResult(key, target, SyncOutcome.DUPLICATE, "ya estaba registrado");
    }

    public static SyncResult rejected(String key, SyncTarget target, String message) {
        return new SyncResult(key, target, SyncOutcome.REJECTED, message);
    }
}
