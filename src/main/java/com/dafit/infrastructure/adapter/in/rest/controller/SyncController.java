package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.port.in.SyncBatchUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.SyncBatchRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.SyncBatchResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sincronizacion offline (C-17).
 *
 * <p>Siempre responde 200: los fallos van por item en {@code results}. Un 4xx
 * global solo indica que el lote entero esta mal formado.
 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncBatchUseCase sync;

    public SyncController(SyncBatchUseCase sync) {
        this.sync = sync;
    }

    @PostMapping("/batch")
    public SyncBatchResponse batch(Authentication authentication,
                                   @Valid @RequestBody SyncBatchRequest request) {
        return ResponseMapper.syncBatch(sync.process(
                Actors.actorId(authentication), RequestMapper.toSyncBatch(request)));
    }
}
