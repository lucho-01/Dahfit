package com.dafit.domain.service;

import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.BenchmarkPoint;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Percentile;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Interpolacion de percentil (§7.2).
 *
 * <pre>
 *   P = P1 + ((e1RM - RM1) / (RM2 - RM1)) x (P2 - P1)
 * </pre>
 *
 * <p>Reglas duras aplicadas:
 * <ul>
 *   <li>Solo se interpola dentro de la misma tabla (variante, categoria, banda
 *       de peso corporal). Mezclar tablas es un error de uso, no un caso raro.</li>
 *   <li>Fuera de los limites NO se extrapola: se devuelve el estado P5/P95.</li>
 *   <li>Sin fuente licenciada no hay percentil. Nunca se fabrica (§7.2).</li>
 * </ul>
 *
 * <p><b>C-06:</b> la interpolacion lineal tiene sesgo en las colas porque la
 * fuerza poblacional no es lineal entre percentiles. La tolerancia aceptada
 * debe fijarse con un conjunto de validacion antes de emitir rangos oficiales.
 */
public class PercentileInterpolator {

    public record Interpolation(AssessmentStatus status, Optional<Percentile> percentile) {

        public Interpolation {
            Objects.requireNonNull(status, "el estado es obligatorio");
            percentile = percentile == null ? Optional.empty() : percentile;
        }

        public static Interpolation official(Percentile percentile) {
            return new Interpolation(AssessmentStatus.OK, Optional.of(percentile));
        }

        public static Interpolation of(AssessmentStatus status) {
            return new Interpolation(status, Optional.empty());
        }

        public boolean isOfficial() {
            return status == AssessmentStatus.OK;
        }
    }

    public Interpolation interpolate(BenchmarkTable table, Kg e1rm) {
        if (e1rm == null || !e1rm.isPositive()) {
            return Interpolation.of(AssessmentStatus.SIN_DATOS);
        }
        if (table == null || !table.isUsable()) {
            return Interpolation.of(AssessmentStatus.SIN_FUENTE);
        }

        List<BenchmarkPoint> points = table.sortedPoints();
        if (points.isEmpty()) {
            return Interpolation.of(AssessmentStatus.SIN_FUENTE);
        }

        BenchmarkPoint lowest = points.get(0);
        BenchmarkPoint highest = points.get(points.size() - 1);

        if (e1rm.compareTo(lowest.e1rm()) < 0) {
            return Interpolation.of(AssessmentStatus.FUERA_DE_TABLA_P5);
        }
        if (e1rm.compareTo(highest.e1rm()) > 0) {
            return Interpolation.of(AssessmentStatus.FUERA_DE_TABLA_P95);
        }

        for (BenchmarkPoint point : points) {
            if (point.e1rm().compareTo(e1rm) == 0) {
                return Interpolation.official(point.percentile());
            }
        }

        for (int i = 0; i < points.size() - 1; i++) {
            BenchmarkPoint lower = points.get(i);
            BenchmarkPoint upper = points.get(i + 1);

            boolean inside = e1rm.compareTo(lower.e1rm()) >= 0 && e1rm.compareTo(upper.e1rm()) <= 0;
            if (!inside) {
                continue;
            }

            BigDecimal rm1 = lower.e1rm().value();
            BigDecimal rm2 = upper.e1rm().value();
            BigDecimal p1 = lower.percentile().value();
            BigDecimal p2 = upper.percentile().value();

            BigDecimal ratio = e1rm.value().subtract(rm1)
                    .divide(rm2.subtract(rm1), MathContext.DECIMAL64);

            BigDecimal percentile = p1.add(ratio.multiply(p2.subtract(p1)));
            return Interpolation.official(Percentile.of(percentile));
        }

        // Inalcanzable con la validacion de monotonicidad de BenchmarkTable.
        return Interpolation.of(AssessmentStatus.SIN_FUENTE);
    }
}
