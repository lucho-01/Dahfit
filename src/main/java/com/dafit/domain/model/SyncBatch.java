package com.dafit.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lote de operaciones offline de un usuario.
 *
 * <p>Se procesa en orden: si el cliente envia el cierre antes que las series,
 * las series posteriores se rechazan. El limite de 200 evita que un lote
 * enorme bloquee la peticion; el cliente puede partir y reintentar.
 */
public record SyncBatch(UUID userId, List<SyncItem> items) {

    public static final int MAX_ITEMS = 200;

    public SyncBatch {
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("el lote no puede estar vacio");
        }
        if (items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("el lote supera el maximo de " + MAX_ITEMS + " items");
        }
        items = List.copyOf(items);
    }
}
