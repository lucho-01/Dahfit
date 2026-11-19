package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.util.UUID;

/** Respuesta minima de toda operacion que crea un recurso. */
public record IdResponse(UUID id) {

    public static IdResponse of(UUID id) {
        return new IdResponse(id);
    }
}
