package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de credenciales para un usuario existente.
 *
 * <p>{@code claimCode} es obligatorio para reemplazar las credenciales de una
 * cuenta pendiente que creo otra persona (R-02).
 */
public record RegisterCredentialsRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        String claimCode
) {
}
