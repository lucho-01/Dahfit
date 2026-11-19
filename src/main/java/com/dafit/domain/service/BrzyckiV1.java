package com.dafit.domain.service;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.E1RM;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Reps;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Formula de Brzycki: {@code e1RM = carga x 36 / (37 - reps)}.
 *
 * <p>No esta aprobada por la especificacion. Existe para demostrar que el
 * versionado del algoritmo funciona: si manana se cambia la formula, se anade
 * una implementacion y las evaluaciones antiguas se siguen reproduciendo con
 * la suya (C-13).
 */
public class BrzyckiV1 implements E1RmStrategy {

    private static final BigDecimal THIRTY_SIX = BigDecimal.valueOf(36);
    private static final BigDecimal THIRTY_SEVEN = BigDecimal.valueOf(37);

    @Override
    public AlgorithmVersion version() {
        return AlgorithmVersion.BRZYCKI_V1;
    }

    @Override
    public E1RM estimate(Kg totalLoad, Reps reps) {
        if (totalLoad == null || !totalLoad.isPositive()) {
            throw new DomainException.NotEligible("la carga debe ser positiva");
        }
        if (reps == null || !reps.isPositive()) {
            throw new DomainException.NotEligible("la serie necesita al menos una repeticion");
        }
        if (reps.value() >= 37) {
            throw new DomainException.NotEligible("Brzycki no es aplicable con 37 o mas repeticiones");
        }
        if (reps.value() == 1) {
            return E1RM.of(totalLoad);
        }

        BigDecimal denominator = THIRTY_SEVEN.subtract(BigDecimal.valueOf(reps.value()));
        BigDecimal factor = THIRTY_SIX.divide(denominator, MathContext.DECIMAL64);

        return E1RM.of(totalLoad.times(factor));
    }
}
