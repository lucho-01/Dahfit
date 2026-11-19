package com.dafit.domain.service;

import com.dafit.domain.model.Points;

import java.util.List;
import java.util.Optional;

/**
 * Estrategia de agregacion de aportes por musculo.
 *
 * <p>Se declara como puerto interno porque la regla esta <b>pendiente (C-09)</b>.
 * La implementacion activa se elige por configuracion, no por una constante
 * enterrada en el codigo.
 */
public interface MuscleAggregationStrategy {

    String id();

    /**
     * @return vacio si no hay aportes utilizables.
     */
    Optional<Points> aggregate(List<MuscleContribution> contributions);
}
