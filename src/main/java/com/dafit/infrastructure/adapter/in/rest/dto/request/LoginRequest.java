package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Login con email y contrasena.
 *
 * <p>{@code claimCode} solo hace falta la primera vez que entra el duenio de
 * una cuenta creada por otra persona (R-02).
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password,
        String claimCode
) {
}
