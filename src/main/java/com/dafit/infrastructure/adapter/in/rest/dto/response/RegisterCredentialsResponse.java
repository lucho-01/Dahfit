package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.util.UUID;

/**
 * Resultado del alta de credenciales.
 *
 * <p>{@code claimCode} solo viaja cuando la cuenta queda pendiente de
 * reclamacion (R-02) y se devuelve una unica vez. Si la cuenta la creo su
 * propio duenio, no hay codigo.
 */
public record RegisterCredentialsResponse(
        UUID userId,
        boolean pendingClaim,
        String claimCode
) {
}
