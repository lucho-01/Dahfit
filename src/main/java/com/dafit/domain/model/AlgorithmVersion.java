package com.dafit.domain.model;

/**
 * Version del algoritmo de calculo (§7.4 - "version de algoritmo").
 *
 * <p>Se guarda en cada evaluacion para que un resultado historico se pueda
 * reproducir exactamente, incluso despues de cambiar la formula (C-13).
 */
public enum AlgorithmVersion {
    EPLEY_V1,
    BRZYCKI_V1
}
