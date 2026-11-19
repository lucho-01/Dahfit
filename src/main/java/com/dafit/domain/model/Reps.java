package com.dafit.domain.model;

/**
 * Repeticiones de una serie.
 *
 * <p>La elegibilidad oficial vive aqui y no en un {@code if} suelto, para que
 * cualquier capa aplique la misma regla. §7.1.
 *
 * <p><b>C-02 PENDIENTE:</b> el rango oficial 1-10 mezcla niveles de error del
 * estimador. Si se aprueba la ventana unica 2-6, basta cambiar las constantes.
 */
public record Reps(int value) {

    public static final int OFFICIAL_MIN = 1;
    public static final int OFFICIAL_MAX = 10;
    public static final int HIGH_CONFIDENCE_MIN = 2;
    public static final int HIGH_CONFIDENCE_MAX = 6;

    public Reps {
        if (value < 0) {
            throw new IllegalArgumentException("las repeticiones no pueden ser negativas: " + value);
        }
    }

    public static Reps of(int value) {
        return new Reps(value);
    }

    public boolean isPositive() {
        return value > 0;
    }

    /** §7.1 - serie de trabajo valida para estimacion oficial. */
    public boolean isOfficialRange() {
        return value >= OFFICIAL_MIN && value <= OFFICIAL_MAX;
    }

    /** Ventana de alta fiabilidad del estimador (base de C-02 y C-11). */
    public boolean isHighConfidence() {
        return value >= HIGH_CONFIDENCE_MIN && value <= HIGH_CONFIDENCE_MAX;
    }
}
