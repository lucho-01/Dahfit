package com.dafit.application.usecase;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Bandas de edad de la categoria de referencia (R-04).
 *
 * <p>Se calculan del perfil, nunca del cuerpo de la peticion. Las etiquetas
 * ("18-29", "30-39"...) son las que esperan las tablas; si una fuente publica
 * otras, se anade su lista y se selecciona por configuracion.
 */
public final class AgeBands {

    /** Bandas de la especificacion; la primera incluye a los menores (no hay tablas infantiles). */
    public static final List<String> DEFAULT = List.of("16-29", "30-39", "40-49", "50-59", "60+");

    private AgeBands() {
    }

    /** Etiqueta de banda para la edad a fecha de {@code at}, o {@code null} si no hay fecha de nacimiento. */
    public static String labelFor(LocalDate birthDate, Instant at) {
        if (birthDate == null) {
            return null;
        }
        LocalDate reference = at == null
                ? LocalDate.now(ZoneOffset.UTC)
                : LocalDate.ofInstant(at, ZoneOffset.UTC);
        int age = Period.between(birthDate, reference).getYears();
        if (age < 0) {
            return null;
        }
        return switch (age) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
                 23, 24, 25, 26, 27, 28, 29 -> "16-29";
            case 30, 31, 32, 33, 34, 35, 36, 37, 38, 39 -> "30-39";
            case 40, 41, 42, 43, 44, 45, 46, 47, 48, 49 -> "40-49";
            case 50, 51, 52, 53, 54, 55, 56, 57, 58, 59 -> "50-59";
            default -> "60+";
        };
    }
}
