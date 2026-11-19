package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Tabla de referencia de un ejercicio (§6 - {@code BenchmarkTable}).
 *
 * <p>Cada tabla corresponde a un ejercicio, una fuente, una categoria y una
 * banda de peso corporal. La interpolacion por peso corporal se hace entre
 * tablas hermanas, y solo si la fuente lo permite (C-07).
 *
 * <p><b>§9:</b> una tabla sin licencia no es utilizable. {@link #isUsable()}
 * lo hace explicito para que ningun flujo pueda saltarselo por descuido.
 */
public record BenchmarkTable(
        UUID id,
        UUID exerciseId,
        String source,
        String sourceVersion,
        BenchmarkCategory category,
        Kg bodyweightKg,
        boolean licensed,
        boolean bodyweightInterpolationAllowed,
        List<BenchmarkPoint> points
) {

    public BenchmarkTable {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(exerciseId, "el ejercicio es obligatorio");
        Objects.requireNonNull(category, "la categoria es obligatoria");
        source = source == null || source.isBlank() ? "DESCONOCIDA" : source;
        sourceVersion = sourceVersion == null || sourceVersion.isBlank() ? "v1" : sourceVersion;
        points = points == null ? List.of() : List.copyOf(points);
        validateMonotonic(points);
    }

    private static void validateMonotonic(List<BenchmarkPoint> points) {
        List<BenchmarkPoint> sorted = points.stream()
                .sorted(Comparator.comparing(BenchmarkPoint::percentile))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).e1rm().compareTo(sorted.get(i - 1).e1rm()) <= 0) {
                throw new DomainException.Validation(
                        "tabla de referencia no monotona: el e1RM debe crecer con el percentil");
            }
        }
    }

    /** Solo una tabla licenciada, con categoria util y con puntos puede emitir percentiles. */
    public boolean isUsable() {
        return licensed && category.isUsable() && !points.isEmpty();
    }

    /**
     * Preferencia entre candidatas de la misma categoria (R-16).
     *
     * <p>Orden: licenciada antes que no licenciada; luego banda exacta de peso
     * corporal antes que tabla generica; luego la version mas reciente.
     */
    public static Comparator<BenchmarkTable> selectionOrder() {
        Comparator<BenchmarkTable> byLicense =
                Comparator.comparing(BenchmarkTable::licensed, Comparator.reverseOrder());
        Comparator<BenchmarkTable> byBand = Comparator.comparing(
                table -> table.bodyweightKg() == null, Comparator.naturalOrder());
        Comparator<BenchmarkTable> byVersion =
                Comparator.comparing(BenchmarkTable::sourceVersion, BenchmarkTable::compareVersions);

        return byLicense.thenComparing(byBand).thenComparing(byVersion.reversed());
    }

    /** Compara dos versiones de fuente: numerica si ambas lo son ("v12" &gt; "v2"), si no alfabetica. */
    public static int compareVersions(String left, String right) {
        String a = left == null ? "" : left.replaceAll("[^0-9]", "");
        String b = right == null ? "" : right.replaceAll("[^0-9]", "");
        if (!a.isEmpty() && !b.isEmpty() && a.length() <= 9 && b.length() <= 9) {
            return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
        }
        return String.valueOf(left).compareTo(String.valueOf(right));
    }

    public List<BenchmarkPoint> sortedPoints() {
        return points.stream()
                .sorted(Comparator.comparing(BenchmarkPoint::percentile))
                .toList();
    }

    public String versionTag() {
        return source + "/" + sourceVersion;
    }
}
