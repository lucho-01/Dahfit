package com.dafit.domain.service;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.E1RM;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Reps;

/**
 * Puerto del algoritmo de estimacion de 1RM.
 *
 * <p>Es un patron Strategy versionado: cada version concreta es inmutable y se
 * elige por su {@link AlgorithmVersion}. Esto es lo que permite reproducir una
 * evaluacion historica sin ambiguedad (§7.4, C-13).
 */
public interface E1RmStrategy {

    AlgorithmVersion version();

    /**
     * @param totalLoad carga total movilizada, ya resuelta segun el
     *                  {@code LoadMode} (§7.1). Para mancuernas es el total,
     *                  no el peso por mano.
     * @param reps      repeticiones de la serie
     */
    E1RM estimate(Kg totalLoad, Reps reps);
}
