package com.dafit.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Historial inmutable de los resultados de aplicar un lote offline. */
public record SyncBatchHistory(
        UUID id,
        UUID userId,
        Instant receivedAt,
        List<SyncResult> results
) {

    public SyncBatchHistory {
        Objects.requireNonNull(id, "el id del lote es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(receivedAt, "la fecha de recepcion es obligatoria");
        if (results == null || results.isEmpty()) {
            throw new IllegalArgumentException("el historial debe contener resultados");
        }
        results = List.copyOf(results);
    }
}
