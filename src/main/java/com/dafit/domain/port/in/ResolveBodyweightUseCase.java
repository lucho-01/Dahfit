package com.dafit.domain.port.in;

import com.dafit.domain.model.Kg;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Puerto de entrada: resolver el peso corporal aplicable a una evaluacion.
 *
 * <p>Implementa C-05 sobre la fecha de medicion (R-11): pesada mas cercana
 * dentro de la ventana, medida en dias de calendario alrededor de la fecha de la
 * evaluacion en la zona horaria del usuario. Sin pesada en ventana, NO se usa un
 * valor lejano: se lanza {@code DomainException.InsufficientData} y la
 * evaluacion queda "Sin evaluar".
 */
public interface ResolveBodyweightUseCase {

    /** @param at instante de la evaluacion ({@code null} = ahora) */
    Kg resolve(UUID userId, Instant at);

    /**
     * Fecha de calendario de la evaluacion en la zona del usuario.
     *
     * <p>Es la base de la ventana C-05 y de los cortes diarios/semanales de la
     * gamificacion: un entrenamiento del domingo a las 21:00 en Bogota pertenece
     * a esa semana aunque en UTC ya sea lunes (C-18).
     */
    LocalDate referenceDate(UUID userId, Instant at);
}
