package com.dafit.domain.service;

import com.dafit.domain.model.E1RM;
import com.dafit.domain.model.Kg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Mejora entre dos evaluaciones (§7.2, §7.5, C-11).
 *
 * <p>El documento fija un umbral del 5 %. El problema es que el error del
 * estimador tambien ronda ese orden, asi que un 5 % puede ser simplemente ruido.
 * Por eso se distinguen dos conceptos:
 *
 * <ul>
 *   <li><b>candidate</b>: supera el 5 % de la especificacion.</li>
 *   <li><b>confirmed</b>: ademas supera el doble del error asumido y ambas series
 *       estan en la ventana de alta fiabilidad.</li>
 * </ul>
 *
 * <p><b>C-11 PENDIENTE:</b> los numeros concretos (5 %, 3 %, ventana) requieren
 * aprobacion y calibracion con datos reales.
 */
public class ImprovementCalculator {

    public record Result(
            Kg delta,
            BigDecimal percent,
            boolean candidate,
            boolean confirmed,
            String note
    ) {
        public Result {
            Objects.requireNonNull(delta, "el delta es obligatorio");
            Objects.requireNonNull(percent, "el porcentaje es obligatorio");
            note = note == null ? "" : note;
        }

        public boolean isDecline() {
            return percent.signum() < 0;
        }
    }

    private final BigDecimal thresholdPercent;
    private final BigDecimal estimatorErrorPercent;
    private final boolean requireHighConfidence;

    public ImprovementCalculator(BigDecimal thresholdPercent,
                                  BigDecimal estimatorErrorPercent,
                                  boolean requireHighConfidence) {
        this.thresholdPercent = thresholdPercent;
        this.estimatorErrorPercent = estimatorErrorPercent;
        this.requireHighConfidence = requireHighConfidence;
    }

    /**
     * @param bothHighConfidence ambas series dentro de la ventana de 2-6 reps
     */
    public Result between(E1RM initial, E1RM current, boolean bothHighConfidence) {
        Objects.requireNonNull(initial, "el e1RM inicial es obligatorio");
        Objects.requireNonNull(current, "el e1RM actual es obligatorio");

        BigDecimal percent = current.improvementPercentOver(initial)
                .setScale(2, RoundingMode.HALF_UP);
        Kg delta = current.deltaOver(initial);

        boolean candidate = percent.compareTo(thresholdPercent) >= 0;

        BigDecimal noiseFloor = initial.value().value()
                .multiply(estimatorErrorPercent)
                .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)
                .multiply(BigDecimal.valueOf(2));

        boolean beyondNoise = delta.value().compareTo(noiseFloor) > 0;
        boolean confidenceOk = !requireHighConfidence || bothHighConfidence;

        boolean confirmed = candidate && beyondNoise && confidenceOk;

        String note;
        if (!candidate) {
            note = "mejora por debajo del umbral del " + thresholdPercent.toPlainString() + "%";
        } else if (!beyondNoise) {
            note = "mejora dentro del margen de variacion del estimador";
        } else if (!confidenceOk) {
            note = "mejora pendiente de series en la ventana de alta fiabilidad (2-6 reps)";
        } else {
            note = "mejora confirmada";
        }

        return new Result(delta, percent, candidate, confirmed, note);
    }
}
