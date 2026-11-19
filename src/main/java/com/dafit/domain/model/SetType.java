package com.dafit.domain.model;

/**
 * Tipo de serie (§6 - {@code Set.tipo_de_serie}).
 *
 * <p>Solo las series de trabajo entran en la estimacion oficial. Una serie al
 * fallo se registra, pero no se premia (ver §8: no incentivar el fallo
 * muscular permanente).
 */
public enum SetType {
    WORKING,
    WARMUP,
    BACKOFF,
    DROP,
    AMRAP,
    FAILURE
}
