package com.dafit.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Objects;

/**
 * Puntos DAHIAN de fuerza. Escala cerrada 0-1000 (§7.2).
 *
 * <p>Es un tipo distinto de {@code Percentile} a proposito: los puntos son la
 * unidad de juego, el percentil es la medida estadistica. Confundirlos es el
 * error mas facil de cometer en este dominio.
 */
public record Points(int value) implements Comparable<Points> {

    public static final int MIN = 0;
    public static final int MAX = 1000;

    public Points {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("puntos fuera de 0-1000: " + value);
        }
    }

    public static Points of(int value) {
        return new Points(value);
    }

    public static Points zero() {
        return new Points(MIN);
    }

    /** Media aritmetica con redondeo HALF_UP (C-12). */
    public static Points average(Collection<Points> values) {
        Objects.requireNonNull(values, "valores obligatorios");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("no se puede promediar una coleccion vacia");
        }
        int sum = values.stream().mapToInt(Points::value).sum();
        BigDecimal mean = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(values.size()), 0, RoundingMode.HALF_UP);
        return new Points(mean.intValueExact());
    }

    public boolean isAtLeast(int threshold) {
        return value >= threshold;
    }

    @Override
    public int compareTo(Points other) {
        return Integer.compare(value, other.value);
    }
}
