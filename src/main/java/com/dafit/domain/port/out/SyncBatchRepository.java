package com.dafit.domain.port.out;

import com.dafit.domain.model.SyncBatchHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: historial persistente de lotes de sincronizacion. */
public interface SyncBatchRepository {
    Optional<SyncBatchHistory> findById(UUID id);
    SyncBatchHistory save(SyncBatchHistory history);
    boolean existsById(UUID id);
    List<SyncBatchHistory> findByUserId(UUID userId);
}