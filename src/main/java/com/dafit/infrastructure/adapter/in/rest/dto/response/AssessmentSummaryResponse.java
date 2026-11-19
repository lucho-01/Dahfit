package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

/**
 * Estado de evaluacion del usuario.
 *
 * <p>{@code global} es nulo mientras el usuario no haya pedido el rango general;
 * Jackson lo omite por {@code non_null}. Las listas nunca son nulas.
 */
public record AssessmentSummaryResponse(
        List<ExerciseAssessmentResponse> exercises,
        List<MuscleAssessmentResponse> muscles,
        GlobalAssessmentResponse global
) {
}
