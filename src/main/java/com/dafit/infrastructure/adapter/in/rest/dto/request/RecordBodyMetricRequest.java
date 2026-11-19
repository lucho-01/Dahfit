package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro de una pesada. */
public record RecordBodyMetricRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weightKg,
        @NotNull LocalDate date
) {
}
