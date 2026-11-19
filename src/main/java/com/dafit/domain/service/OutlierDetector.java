package com.dafit.domain.service;

import com.dafit.domain.model.E1RM;
import com.dafit.domain.model.Kg;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

/**
 * Deteccion de registros implausibles (C-16, §9 "confirmacion de carga atipica").
 *
 * <p>No borra datos: solo impide que una serie sospechosa entre al calculo
 * oficial. El usuario puede confirmarla y entonces se re-evalua.
 *
 * <p><b>C-16 PENDIENTE:</b> los umbrales son heuristicos. Se calibran cuando
 * exista un historico real.
 */
public class OutlierDetector {

    public record Verdict(boolean suspicious, String reason) {

        public static Verdict ok() {
            return new Verdict(false, "");
        }

        public static Verdict suspicious(String reason) {
            return new Verdict(true, reason);
        }
    }

    private final BigDecimal maxMultipleOfHistory;
    private final BigDecimal maxE1rmJumpPercent;

    public OutlierDetector(BigDecimal maxMultipleOfHistory, BigDecimal maxE1rmJumpPercent) {
        this.maxMultipleOfHistory = maxMultipleOfHistory;
        this.maxE1rmJumpPercent = maxE1rmJumpPercent;
    }

    /**
     * Regla 1: una carga muy superior al maximo historico del usuario.
     */
    public Verdict checkLoad(Kg load, Optional<Kg> historicalMax) {
        if (load == null || !load.isPositive()) {
            return Verdict.suspicious("la carga no es positiva");
        }
        if (historicalMax.isEmpty()) {
            return Verdict.ok();
        }
        BigDecimal limit = historicalMax.get().value().multiply(maxMultipleOfHistory);
        if (load.value().compareTo(limit) > 0) {
            return Verdict.suspicious(
                    "carga " + load + " superior a " + maxMultipleOfHistory.toPlainString()
                            + "x el maximo historico (" + historicalMax.get() + ")");
        }
        return Verdict.ok();
    }

    /**
     * Regla 2: salto brusco de e1RM entre sesiones consecutivas.
     */
    public Verdict checkE1rmJump(Optional<E1RM> previous, E1RM candidate) {
        if (previous.isEmpty() || candidate == null) {
            return Verdict.ok();
        }
        BigDecimal jump = candidate.improvementPercentOver(previous.get());
        if (jump.compareTo(maxE1rmJumpPercent) > 0) {
            return Verdict.suspicious(
                    "salto de e1RM del " + jump.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()
                            + "% en una sesion");
        }
        return Verdict.ok();
    }
}
