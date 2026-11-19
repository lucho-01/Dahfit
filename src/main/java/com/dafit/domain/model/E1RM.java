package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * 1RM estimado (§7.2).
 *
 * <p>Es una estimacion, no una medicion. El error del modelo es la razon por la
 * que C-11 exige que una mejora supere el ruido del estimador.
 */
public record E1RM(Kg value) implements Comparable<E1RM> {

    public E1RM {
        Objects.requireNonNull(value, "el e1RM es obligatorio");
        if (!value.isPositive()) {
            throw new DomainException.Validation("el e1RM debe ser positivo: " + value);
        }
    }

    public static E1RM of(Kg value) {
        return new E1RM(value);
    }

    public boolean beats(E1RM other) {
        return value.compareTo(other.value) > 0;
    }

    /** §7.2 - {@code 100 x (final - inicial) / inicial}. */
    public BigDecimal improvementPercentOver(E1RM initial) {
        return value.value()
                .subtract(initial.value().value())
                .divide(initial.value().value(), MathContext.DECIMAL64)
                .multiply(BigDecimal.valueOf(100));
    }

    public Kg deltaOver(E1RM initial) {
        return value.minus(initial.value());
    }

    @Override
    public int compareTo(E1RM other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return "e1RM=" + value;
    }
}
