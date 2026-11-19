package com.dafit.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evaluacion de un ejercicio.
 *
 * <p>Hay dos formas de aportar la serie:
 * <ul>
 *   <li>{@code bestSetId}: el servidor carga la serie del historial. En este
 *       caso {@code totalLoadKg}, {@code reps} y {@code rir} se ignoran.</li>
 *   <li>Entrada manual: {@code totalLoadKg}, {@code reps} y {@code rir}.</li>
 * </ul>
 *
 * <p>Por eso los campos de la serie manual son opcionales: exigirlos siempre
 * obligaba al cliente a reenviar datos que el servidor ya tiene y que, si no
 * coincidian con la serie real, quedaban descartados en silencio. La ausencia
 * de ambos caminos produce {@code SIN_DATOS}, no un 400.
 *
 * <p>{@code sex} y {@code ageBand} se aceptan por compatibilidad, pero el
 * servidor NO los usa: la categoria se deriva del perfil (R-04).
 */
public record EvaluateExerciseRequest(
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal totalLoadKg,
        Integer reps,
        Integer rir,
        String sex,
        String ageBand,
        String algorithmVersion,
        UUID bestSetId,
        boolean confirmOutlier,
        Instant assessedAt
) {
}
