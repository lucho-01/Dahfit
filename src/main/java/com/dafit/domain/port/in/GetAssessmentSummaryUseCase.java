package com.dafit.domain.port.in;

import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.MuscleAssessment;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de entrada: estado de evaluacion de un usuario (§7).
 *
 * <p>Devuelve la ultima evaluacion de cada ejercicio y de cada musculo, mas el
 * ultimo rango general. Es lo que necesita la pantalla de progreso: el
 * historico completo no aporta nada al usuario y solo hace crecer la respuesta.
 *
 * <p>{@code actorId} es el usuario autenticado ({@code null} si el API esta
 * abierto): el caso de uso solo responde al propio usuario o a un ADMIN (R-03).
 */
public interface GetAssessmentSummaryUseCase {

    Summary summaryFor(UUID actorId, UUID userId);

    /**
     * @param global {@code null} si el usuario nunca ha solicitado el rango general
     */
    record Summary(
            List<ExerciseAssessment> exercises,
            List<MuscleAssessment> muscles,
            GlobalAssessment global
    ) {
    }
}
