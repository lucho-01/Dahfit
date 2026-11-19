package com.dafit.domain.model;

/** Tipos de evento que otorgan XP (C-19). */
public enum XpEventType {

    /** Sesion de entrenamiento completada. */
    SESSION_COMPLETED,

    /** Bonus por la primera sesion completada de la semana ISO. */
    FIRST_SESSION_OF_WEEK,

    /** Revision de ciclo de cuatro semanas, una sola vez por ciclo. */
    FOUR_WEEK_REVIEW
}
