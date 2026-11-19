package com.dafit.domain.model;

/**
 * Repeticiones en reserva (RIR) de una serie.
 *
 * <p><b>C-03:</b> en el MVP el RIR NO entra en la formula de e1RM, solo decide
 * elegibilidad (opcion A aprobada por defecto). Si se migra a reps efectivas,
 * el cambio vive en {@code E1RmStrategy}, no aqui.
 *
 * <p><b>C-04:</b> se acepta registrar 0-5, pero solo 0-4 es elegible. Elimina la
 * contradiccion entre §7.1 (0-2) y §9 (0-4).
 */
public record Rir(int value) {

    public static final int MIN_LOGGABLE = 0;
    public static final int MAX_LOGGABLE = 5;
    public static final int MAX_OFFICIAL = 4;

    public Rir {
        if (value < MIN_LOGGABLE || value > MAX_LOGGABLE) {
            throw new IllegalArgumentException(
                    "RIR fuera del rango registrable (" + MIN_LOGGABLE + "-" + MAX_LOGGABLE + "): " + value);
        }
    }

    public static Rir of(int value) {
        return new Rir(value);
    }

    public boolean isOfficialRange() {
        return value <= MAX_OFFICIAL;
    }
}
