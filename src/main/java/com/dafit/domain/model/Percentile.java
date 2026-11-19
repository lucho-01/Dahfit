package com.dafit.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Percentil de fuerza (0-100) frente a una tabla de referencia.
 *
 * <p>Escala 2 decimales, HALF_UP (C-12). Nunca se fabrica un percentil: si no hay
 * fuente, el flujo termina en {@code AssessmentStatus.SIN_FUENTE} (§7.2).
 */
public record Percentile(BigDecimal value) implements Comparable<Percentile> {

    public static final BigDecimal MIN = BigDecimal.ZERO;
    public static final BigDecimal MAX = new BigDecimal("100");
    public static final int SCALE = 2;

    public Percentile {
        Objects.requireNonNull(value, "el percentil es obligatorio");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
        if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
            throw new IllegalArgumentException("percentil fuera de 0-100: " + value);
        }
    }

    public static Percentile of(BigDecimal value) {
        return new Percentile(value);
    }

    public static Percentile of(double value) {
        return new Percentile(BigDecimal.valueOf(value));
    }

    /** §7.2 - {@code puntos = percentil x 10}, rango 0-1000 (C-12: redondeo HALF_UP). */
    public Points toPoints() {
        BigDecimal raw = value.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP);
        return Points.of(raw.intValueExact());
    }

    /** Distancia entre dos percentiles, en puntos porcentuales. */
    public BigDecimal distanceTo(Percentile other) {
        return value.subtract(other.value).abs();
    }

    @Override
    public int compareTo(Percentile other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toPlainString() + " p";
    }
}
