package com.dafit.domain.port.in;

import com.dafit.domain.model.SyncBatch;
import com.dafit.domain.model.SyncResult;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: aplicar un lote de operaciones offline.
 *
 * <p>Nunca lanza por un item fallido: devuelve un resultado por item para que el
 * cliente sepa que puede borrar de su cola y que debe reintentar o descartar.
 */
public interface SyncBatchUseCase {

    /**
     * @param actorId usuario autenticado ({@code null} con el API abierto); el
     *                lote solo puede pertenecer a ese usuario
     */
    List<SyncResult> process(UUID actorId, SyncBatch batch);
}
