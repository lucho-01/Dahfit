package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Alta de usuario. */
public record RegisterUserRequest(
        @NotBlank String displayName,
        @NotNull String role,
        String sex,
        LocalDate birthDate,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal bodyweightKg,
        String preferredUnit,
        String timeZone,
        boolean benchmarkConsent
) {
}
