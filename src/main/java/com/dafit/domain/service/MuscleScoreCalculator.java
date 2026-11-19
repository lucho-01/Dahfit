package com.dafit.domain.service;

import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Assessment;
import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.RankScale;
import com.dafit.domain.model.Points;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Calcula la evaluacion de un grupo muscular (§7.4, C-14).
 *
 * <p>Reglas aplicadas:
 * <ol>
 *   <li>Solo cuentan evaluaciones de ejercicio con estado oficial.</li>
 *   <li>Sin evaluaciones del musculo: {@code SIN_DATOS}, nunca 0 puntos.</li>
 *   <li>Sin al menos una prueba primaria valida: {@code COBERTURA_INSUFICIENTE}.</li>
 *   <li>Cobertura por debajo del minimo: {@code COBERTURA_INSUFICIENTE}.</li>
 *   <li>La puntuacion se agrega con la estrategia configurada (C-09).</li>
 * </ol>
 *
 * <p><b>Cobertura (R-01):</b> es una fraccion en [0, 1]:
 * {@code cobertura = suma(coeficientes evaluados) / suma(coeficientes
 * disponibles del musculo)}, acotada a 1. Con dos ejercicios (1.000 + 0.700)
 * vale 1.000 si son todo el catalogo del musculo; evaluar un solo ejercicio
 * secundario del par da 0.700 / 1.700 = 0.412 y no supera el minimo.
 */
public class MuscleScoreCalculator {

    /** Escala de la columna {@code muscle_assessments.coverage}. */
    private static final int COVERAGE_SCALE = 3;

    private final MuscleAggregationStrategy aggregation;
    private final RankScale rankScale;
    private final int minPrimaryTests;
    private final BigDecimal minCoverage;

    public MuscleScoreCalculator(MuscleAggregationStrategy aggregation,
                                 RankScale rankScale,
                                 int minPrimaryTests,
                                 BigDecimal minCoverage) {
        this.aggregation = aggregation;
        this.rankScale = rankScale;
        this.minPrimaryTests = minPrimaryTests;
        this.minCoverage = minCoverage;
    }

    /**
     * @param assessments         todas las evaluaciones del usuario (se filtran aqui)
     * @param coefficients        coeficiente de CADA ejercicio disponible del
     *                            catalogo para este musculo (no solo los
     *                            evaluados): es el denominador de la cobertura
     * @param primaryExerciseIds  ejercicios declarados pruebas primarias del musculo
     */
    public MuscleAssessment calculate(UUID userId,
                                      MuscleGroup muscle,
                                      List<ExerciseAssessment> assessments,
                                      Map<UUID, BigDecimal> coefficients,
                                      Set<UUID> primaryExerciseIds,
                                      Instant assessedAt) {

        List<ExerciseAssessment> eligible = assessments.stream()
                .filter(Assessment::isOfficial)
                .filter(a -> a.points().isPresent())
                .filter(a -> coefficients.containsKey(a.exerciseId()))
                .toList();

        if (eligible.isEmpty()) {
            return MuscleAssessment.notEvaluated(
                    userId, muscle, AssessmentStatus.SIN_DATOS,
                    BigDecimal.ZERO, List.of(), 0, assessedAt);
        }

        List<MuscleContribution> contributions = eligible.stream()
                .map(a -> new MuscleContribution(
                        a.exerciseId(), a.points().orElseThrow(), coefficients.get(a.exerciseId())))
                .toList();

        // R-01: la cobertura es la fraccion del musculo cubierta por los
        // ejercicios evaluados. Se normaliza por la suma de coeficientes
        // disponibles para ese musculo y se acota a [0, 1]:
        //   * dos ejercicios (1.000 + 0.700) no pueden dar 1.700, que el
        //     esquema rechazaria (NUMERIC(4,3) con CHECK <= 1);
        //   * evaluar TODO el catalogo del musculo da exactamente 1.000.
        BigDecimal available = coefficients.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal coverage = available.signum() == 0
                ? BigDecimal.ZERO
                : contributions.stream()
                        .map(MuscleContribution::coefficient)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(available, COVERAGE_SCALE, RoundingMode.HALF_UP)
                        .min(BigDecimal.ONE);

        List<UUID> evaluatedIds = eligible.stream()
                .map(ExerciseAssessment::exerciseId)
                .distinct()
                .toList();

        int primaryTests = (int) eligible.stream()
                .filter(a -> primaryExerciseIds.contains(a.exerciseId()))
                .map(ExerciseAssessment::exerciseId)
                .distinct()
                .count();

        if (primaryTests < minPrimaryTests) {
            return MuscleAssessment.notEvaluated(
                    userId, muscle, AssessmentStatus.COBERTURA_INSUFICIENTE,
                    coverage, evaluatedIds, primaryTests, assessedAt);
        }

        if (coverage.compareTo(minCoverage) < 0) {
            return MuscleAssessment.notEvaluated(
                    userId, muscle, AssessmentStatus.COBERTURA_INSUFICIENTE,
                    coverage, evaluatedIds, primaryTests, assessedAt);
        }

        Optional<Points> points = aggregation.aggregate(contributions);
        if (points.isEmpty()) {
            return MuscleAssessment.notEvaluated(
                    userId, muscle, AssessmentStatus.SIN_DATOS,
                    coverage, evaluatedIds, primaryTests, assessedAt);
        }

        return MuscleAssessment.of(
                userId, muscle, AssessmentStatus.OK,
                points.get(), rankScale.classify(points.get()),
                coverage, evaluatedIds, primaryTests, assessedAt);
    }
}
