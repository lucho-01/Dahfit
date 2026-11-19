package com.dafit.domain.port.in;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;

import java.time.Instant;
import java.util.UUID;

/**
 * Puerto de entrada: evaluar un ejercicio (§5, pantalla "Fuerza DAHIAN").
 *
 * <p>Dos formas de aportar la serie:
 * <ol>
 *   <li><b>Desde el historial</b> ({@code bestSetId}): el servidor carga la
 *       serie, comprueba que es del usuario y resuelve la carga total. Es la
 *       forma recomendada: el cliente no puede inflar la carga.</li>
 *   <li><b>Manual</b> ({@code totalLoad}, {@code reps}, {@code rir}): el usuario
 *       declara una serie sin historial. No hay contraste de atipicos porque no
 *       hay serie real (C-16).</li>
 * </ol>
 *
 * <p>La categoria de referencia NO se recibe: se deriva del perfil (R-04).
 */
public interface EvaluateExerciseUseCase {

    ExerciseAssessment evaluate(Command command);

    record Command(
            UUID actorId,
            UUID userId,
            UUID exerciseId,
            UUID bestSetId,
            Kg totalLoad,
            Reps reps,
            Rir rir,
            AlgorithmVersion algorithmVersion,
            boolean confirmOutlier,
            Instant assessedAt
    ) {
    }
}
