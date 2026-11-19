package com.dafit.domain.service;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.E1RM;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Reps;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formula de Epley (§7.2): {@code e1RM = carga x (1 + reps / 30)}.
 *
 * <p><b>C-01 aplicada:</b> con una repeticion la formula devolveria un 3,33 %
 * mas de lo levantado. El caso base {@code reps == 1} devuelve la carga tal cual.
 */
public class EpleyV1 implements E1RmStrategy {

    private static final BigDecimal THIRTY = BigDecimal.valueOf(30);

    @Override
    public AlgorithmVersion version() {
        return AlgorithmVersion.EPLEY_V1;
    }

    @Override
    public E1RM estimate(Kg totalLoad, Reps reps) {
        if (totalLoad == null || !totalLoad.isPositive()) {
            throw new DomainException.NotEligible("la carga debe ser positiva para estimar el e1RM");
        }
        if (reps == null || !reps.isPositive()) {
            throw new DomainException.NotEligible("la serie necesita al menos una repeticion");
        }
        if (reps.value() == 1) {
            return E1RM.of(totalLoad);
        }
        if (!reps.isOfficialRange()) {
            throw new DomainException.NotEligible(
                    "repeticiones fuera del rango oficial (" + Reps.OFFICIAL_MIN
                            + "-" + Reps.OFFICIAL_MAX + "): " + reps.value());
        }

        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps.value()).divide(THIRTY, MathContext.DECIMAL64));

        return E1RM.of(totalLoad.times(factor));
    }
}
