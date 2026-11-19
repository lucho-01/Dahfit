package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Fuerza relativa: e1RM dividido por el peso corporal (§7.2).
 *
 * <p>Se almacena con 4 decimales (C-12) porque es un valor que el usuario ve y
 * compara; la precision extra del estimador no aporta nada visible.
 */
public record RelativeStrength(BigDecimal value) implements Comparable<RelativeStrength> {

    public static final int SCALE = 4;

    public RelativeStrength {
        Objects.requireNonNull(value, "la fuerza relativa es obligatoria");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
        if (value.signum() <= 0) {
            throw new DomainException.Validation("la fuerza relativa debe ser positiva: " + value);
        }
    }

    /**
     * @throws DomainException.InsufficientData si no hay peso corporal valido.
     *         La evaluacion no puede inventar un denominador.
     */
    public static RelativeStrength of(Kg e1rm, Kg bodyweight) {
        if (bodyweight == null || !bodyweight.isPositive()) {
            throw new DomainException.InsufficientData(
                    "sin peso corporal no se puede calcular la fuerza relativa");
        }
        BigDecimal ratio = e1rm.value().divide(bodyweight.value(), MathContext.DECIMAL64);
        return new RelativeStrength(ratio);
    }

    @Override
    public int compareTo(RelativeStrength other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toPlainString() + "x PC";
    }
}
