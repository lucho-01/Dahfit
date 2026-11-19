package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

/** Resultado de un lote de sincronizacion, con totales para la UI. */
public record SyncBatchResponse(
        List<SyncItemResponse> results,
        int applied,
        int duplicates,
        int rejected
) {

    public record SyncItemResponse(
            String key,
            String target,
            String outcome,
            String message
    ) {
    }

    public static SyncBatchResponse of(List<SyncItemResponse> results) {
        int applied = (int) results.stream().filter(r -> "APPLIED".equals(r.outcome())).count();
        int duplicates = (int) results.stream().filter(r -> "DUPLICATE".equals(r.outcome())).count();
        int rejected = (int) results.stream().filter(r -> "REJECTED".equals(r.outcome())).count();
        return new SyncBatchResponse(results, applied, duplicates, rejected);
    }
}
