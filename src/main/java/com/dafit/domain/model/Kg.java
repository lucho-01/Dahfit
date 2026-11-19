package com.dafit.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Peso en kilogramos. Unidad interna unica de todo el sistema.
 *
 * <p>Reglas aplicadas:
 * <ul>
 *   <li>§7.1 - internamente siempre kg; la unidad de origen se conserva en el agregado.</li>
 *   <li>1 lb = 0,45359237 kg.</li>
 *   <li>C-12 - escala 3 decimales (gramo), redondeo HALF_UP. Debe coincidir con
 *       {@code NUMERIC(7, 3)} del esquema: si el dominio conserva mas
 *       precision que la base, una reevaluacion da un resultado distinto al
 *       guardado.</li>
 * </ul>
 */
public record Kg(BigDecimal value) implements Comparable<Kg> {

    public static final int SCALE = 3;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal LB_TO_KG = new BigDecimal("0.45359237");
    public static final Kg ZERO = new Kg(BigDecimal.ZERO);

    public Kg {
        Objects.requireNonNull(value, "el peso es obligatorio");
        value = value.setScale(SCALE, ROUNDING);
    }

    public static Kg ofKg(BigDecimal kg) {
        return new Kg(kg);
    }

    public static Kg ofKg(long kg) {
        return new Kg(BigDecimal.valueOf(kg));
    }

    public static Kg ofKg(String kg) {
        return new Kg(new BigDecimal(kg));
    }

    public static Kg ofLb(BigDecimal lb) {
        return new Kg(lb.multiply(LB_TO_KG));
    }

    public static Kg ofLb(String lb) {
        return ofLb(new BigDecimal(lb));
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    public Kg plus(Kg other) {
        return new Kg(value.add(other.value));
    }

    public Kg minus(Kg other) {
        return new Kg(value.subtract(other.value));
    }

    public Kg times(BigDecimal factor) {
        return new Kg(value.multiply(factor));
    }

    public Kg times(int factor) {
        return new Kg(value.multiply(BigDecimal.valueOf(factor)));
    }

    public Kg half() {
        return new Kg(value.divide(BigDecimal.valueOf(2), SCALE, ROUNDING));
    }

    /** Proporcion frente a otro peso (p. ej. fuerza relativa). */
    public BigDecimal ratioTo(Kg other) {
        return value.divide(other.value, MathContext.DECIMAL64);
    }

    public BigDecimal toLb() {
        return value.divide(LB_TO_KG, 2, ROUNDING);
    }

    @Override
    public int compareTo(Kg other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value.toPlainString() + " kg";
    }
}
