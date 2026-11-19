package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Percentile;

import java.util.Objects;

/**
 * Punto de referencia de una tabla: "para este percentil, este e1RM" (§7.2).
 *
 * <p>Regla de integridad: los puntos de una tabla deben ser estrictamente
 * crecientes en e1RM respecto al percentil. Si no lo son, la tabla tiene un
 * error de transcripcion y debe rechazarse en la carga.
 */
public record BenchmarkPoint(Percentile percentile, Kg e1rm) {

    public BenchmarkPoint {
        Objects.requireNonNull(percentile, "el percentil es obligatorio");
        Objects.requireNonNull(e1rm, "el e1RM de referencia es obligatorio");
        if (!e1rm.isPositive()) {
            throw new DomainException.Validation("el e1RM de referencia debe ser positivo: " + e1rm);
        }
    }

    public static BenchmarkPoint of(double percentile, String e1rmKg) {
        return new BenchmarkPoint(Percentile.of(percentile), Kg.ofKg(e1rmKg));
    }
}
