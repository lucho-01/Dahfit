package com.dafit.domain.model;

/**
 * Categoria de referencia usada por las tablas de percentiles.
 *
 * <p>§4 la deja como decision pendiente. El dominio ya la modela para que la
 * evaluacion pueda quedar en {@code AssessmentStatus.FALTA_CATEGORIA} cuando el
 * usuario no la declare.
 */
public enum Sex {
    MALE,
    FEMALE,
    UNSPECIFIED
}
