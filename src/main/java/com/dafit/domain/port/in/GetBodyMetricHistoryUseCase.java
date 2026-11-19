package com.dafit.domain.port.in;

import com.dafit.domain.model.BodyMetric;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: consultar el historico de peso corporal (§6, C-05). */
public interface GetBodyMetricHistoryUseCase {

    /** Mas reciente primero. Lista vacia si el usuario no ha registrado ninguna pesada. */
    List<BodyMetric> history(UUID actorId, UUID userId);
}
