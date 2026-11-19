package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Points;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Escala de conversion de puntos a rango (§7.3).
 *
 * <p>Se modela como dato, no como cadena de {@code if}, para poder versionarla
 * y recalibrarla sin tocar codigo de negocio (C-10).
 *
 * <p><b>Limite de banda:</b> el corte pertenece al rango superior, es decir
 * 199,4 puntos son PRINCIPIANTE y 199,5 no existen porque los puntos son
 * enteros (C-12).
 */
public record RankScale(String id, List<Band> bands) {

    public record Band(StrengthRank rank, int from, int to) {

        public Band {
            Objects.requireNonNull(rank, "el rango es obligatorio");
            if (from < 0 || to > Points.MAX || from > to) {
                throw new DomainException.Validation(
                        "banda invalida para " + rank + ": " + from + "-" + to);
            }
        }

        public boolean contains(Points points) {
            return points.value() >= from && points.value() <= to;
        }
    }

    public RankScale {
        Objects.requireNonNull(id, "la escala necesita identificador");
        if (bands == null || bands.isEmpty()) {
            throw new DomainException.Validation("la escala necesita al menos una banda");
        }
        bands = bands.stream()
                .sorted(Comparator.comparingInt(Band::from))
                .toList();
    }

    /**
     * Escala original del documento (§7.3), sin cambios.
     *
     * <p>Traduccion real: Principiante P0-20, Novato P20-40, Avanzado P40-60,
     * Bestia P60-80, Elite P80-95, Brutal P95-100.
     */
    public static RankScale specV1() {
        return new RankScale("SPEC_V1", List.of(
                new Band(StrengthRank.PRINCIPIANTE, 0, 199),
                new Band(StrengthRank.NOVATO, 200, 399),
                new Band(StrengthRank.AVANZADO, 400, 599),
                new Band(StrengthRank.BESTIA, 600, 799),
                new Band(StrengthRank.ELITE, 800, 949),
                new Band(StrengthRank.BRUTAL, 950, 1000)));
    }

    /**
     * Propuesta C-10 (opcion B): mismos nombres, cortes honestos.
     *
     * <p>Elite pasa a exigir percentil 90 y Brutal percentil 98. No usar hasta
     * que la decision se apruebe y se actualicen las pruebas de aceptacion.
     */
    public static RankScale recalibratedV1() {
        return new RankScale("RECALIBRATED_V1", List.of(
                new Band(StrengthRank.PRINCIPIANTE, 0, 199),
                new Band(StrengthRank.NOVATO, 200, 499),
                new Band(StrengthRank.AVANZADO, 500, 749),
                new Band(StrengthRank.BESTIA, 750, 899),
                new Band(StrengthRank.ELITE, 900, 979),
                new Band(StrengthRank.BRUTAL, 980, 1000)));
    }

    public StrengthRank classify(Points points) {
        Objects.requireNonNull(points, "los puntos son obligatorios");
        return bands.stream()
                .filter(band -> band.contains(points))
                .map(Band::rank)
                .findFirst()
                .orElseThrow(() -> new DomainException.Validation(
                        "los puntos " + points.value() + " no caen en ninguna banda de " + id));
    }
}
