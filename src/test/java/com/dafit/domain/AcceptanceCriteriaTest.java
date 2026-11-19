package com.dafit.domain;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.BenchmarkPoint;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.E1RM;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Percentile;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.RankScale;
import com.dafit.domain.model.RelativeStrength;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.StrengthRank;
import com.dafit.domain.model.WeightUnit;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutSet;
import com.dafit.domain.service.BestSetSelector;
import com.dafit.domain.service.E1RmStrategyRegistry;
import com.dafit.domain.service.EpleyV1;
import com.dafit.domain.service.GlobalRankCalculator;
import com.dafit.domain.service.ImprovementCalculator;
import com.dafit.domain.service.MaxPointsAggregation;
import com.dafit.domain.service.MuscleScoreCalculator;
import com.dafit.domain.service.PercentileInterpolator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pruebas de aceptacion obligatorias del documento (§11), una por caso.
 *
 * <p>Son las mismas 12 filas de la tabla de la especificacion, traducidas a
 * aserciones. Si una de estas falla, el motor se ha desviado del contrato, no
 * es un detalle cosmético.
 */
class AcceptanceCriteriaTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    private final EpleyV1 epley = new EpleyV1();
    private final PercentileInterpolator interpolator = new PercentileInterpolator();

    // --- Caso 1: 95 lb x 10 en barra --------------------------------------

    @Test
    void conversaLbAKg() {
        Kg load = Kg.ofLb("95");

        assertThat(load.value()).isEqualByComparingTo("43.0913");
    }

    @Test
    void e1rmBarra95LbPor10() {
        E1RM e1rm = epley.estimate(Kg.ofLb("95"), Reps.of(10));

        // 43,0913 x (1 + 10/30) = 57,4551
        assertThat(e1rm.value().value()).isCloseTo(
                new BigDecimal("57.4551"), within(new BigDecimal("0.001")));
    }

    // --- Caso 2: peso corporal 74,20 kg -----------------------------------

    @Test
    void fuerzaRelativa() {
        E1RM e1rm = epley.estimate(Kg.ofLb("95"), Reps.of(10));

        RelativeStrength fr = RelativeStrength.of(e1rm.value(), Kg.ofKg("74.20"));

        // 57,4551 / 74,20 = 0,7743
        assertThat(fr.value()).isEqualByComparingTo("0.7743");
    }

    // --- Caso 3: dos mancuernas de 30 kg x 4 ------------------------------

    @Test
    void mancuernasCargaTotalYE1rm() {
        WorkoutSet set = set(LoadMode.PER_HAND, "30", 2, 4, 0);

        assertThat(set.totalLoad().value()).isEqualByComparingTo("60.0000");

        E1RM total = epley.estimate(set.totalLoad(), set.reps());
        assertThat(total.value().value()).isEqualByComparingTo("68.0000");

        // referencia por mano: 34 kg estimados por mancuerna
        assertThat(total.value().half().value()).isEqualByComparingTo("34.0000");
    }

    // --- Caso 4: dos mancuernas de 20 kg x 8 ------------------------------

    @Test
    void mancuernasE1rmTotalRespetandoUnidad() {
        WorkoutSet set = set(LoadMode.PER_HAND, "20", 2, 8, 0);

        assertThat(set.totalLoad().value()).isEqualByComparingTo("40.0000");

        E1RM total = epley.estimate(set.totalLoad(), set.reps());
        // 40 x (1 + 8/30) = 50,6667 (~50,67 en el documento)
        assertThat(total.value().value()).isCloseTo(
                new BigDecimal("50.6667"), within(new BigDecimal("0.001")));
    }

    // --- Caso 5: 60 kg x 4 vs 50 kg x 10 ----------------------------------

    @Test
    void mejorSerieEsLaDeMayorE1rm() {
        E1RmStrategyRegistry registry = new E1RmStrategyRegistry(List.of(epley));
        BestSetSelector selector = new BestSetSelector(registry);

        WorkoutSet sesentaPorCuatro = set(LoadMode.TOTAL, "60", 1, 4, 0);   // e1RM 68
        WorkoutSet cincuentaPorDiez = set(LoadMode.TOTAL, "50", 1, 10, 0);  // e1RM 66,67

        Optional<WorkoutSet> best = selector.selectBest(
                List.of(cincuentaPorDiez, sesentaPorCuatro), AlgorithmVersion.EPLEY_V1);

        assertThat(best).isPresent();
        assertThat(best.get().load().value()).isEqualByComparingTo("60.0000");
    }

    // --- Caso 6: P50=90, P80=130, e1RM=110 -> percentil 65, 650 pts --------

    @Test
    void interpolacionPercentilYPuntos() {
        BenchmarkTable table = table(List.of(
                BenchmarkPoint.of(50, "90"),
                BenchmarkPoint.of(80, "130")));

        PercentileInterpolator.Interpolation interpolation =
                interpolator.interpolate(table, Kg.ofKg("110"));

        assertThat(interpolation.isOfficial()).isTrue();
        assertThat(interpolation.percentile().get().value()).isEqualByComparingTo("65.00");
        assertThat(interpolation.percentile().get().toPoints().value()).isEqualTo(650);
    }

    // --- Caso 7: puntos 650 -> rango Bestia -------------------------------

    @Test
    void puntos650EsBestia() {
        assertThat(RankScale.specV1().classify(Points.of(650)))
                .isEqualTo(StrengthRank.BESTIA);
    }

    // --- Caso 8: musculo sin prueba primaria valida -----------------------

    @Test
    void musculoSinPruebaPrimariaSinRangoOficial() {
        MuscleScoreCalculator calculator = new MuscleScoreCalculator(
                new MaxPointsAggregation(), RankScale.specV1(), 1, new BigDecimal("0.60"));

        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();

        ExerciseAssessment official = ExerciseAssessment.evaluated(
                userId, exerciseId, E1RM.of(Kg.ofKg("68")), Kg.ofKg("74.20"),
                Percentile.of(65), StrengthRank.BESTIA, AlgorithmVersion.EPLEY_V1,
                UUID.randomUUID(), UUID.randomUUID(), NOW);

        MuscleAssessment result = calculator.calculate(
                userId, MuscleGroup.CHEST,
                List.of(official),
                Map.of(exerciseId, new BigDecimal("0.70")),
                Set.of(), // ninguna prueba primaria declarada
                NOW);

        assertThat(result.status()).isEqualTo(AssessmentStatus.COBERTURA_INSUFICIENTE);
        assertThat(result.rank()).isEmpty();
        assertThat(result.points()).isEmpty();
    }

    // --- Caso 9: 7 de 8 musculos evaluados --------------------------------

    @Test
    void rangoGlobalExigeLosOchoMusculos() {
        GlobalRankCalculator calculator =
                new GlobalRankCalculator(RankScale.specV1(), 8, true);

        UUID userId = UUID.randomUUID();
        List<MuscleAssessment> seven = new ArrayList<>();
        for (MuscleGroup muscle : MuscleGroup.values()) {
            if (muscle == MuscleGroup.GLUTES) {
                continue; // solo 7 de 8
            }
            seven.add(MuscleAssessment.of(
                    userId, muscle, AssessmentStatus.OK,
                    Points.of(650), StrengthRank.BESTIA,
                    new BigDecimal("0.70"), List.of(), 1, NOW));
        }

        GlobalAssessment result = calculator.calculate(userId, seven, NOW);

        assertThat(result.status()).isEqualTo(AssessmentStatus.COBERTURA_INSUFICIENTE);
        assertThat(result.musclesEvaluated()).isEqualTo(7);
        assertThat(result.rank()).isEmpty();
        assertThat(result.points()).isEmpty();
    }

    // --- Caso 10: misma sesion sincronizada dos veces ---------------------

    @Test
    void sincronizarDosVecesNoDuplicaSeries() {
        UUID userId = UUID.randomUUID();
        UUID exerciseId = UUID.randomUUID();
        Workout workout = Workout.start(userId, LocalDate.now(BOGOTA), BOGOTA, NOW);

        WorkoutSet set = new WorkoutSet(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                workout.id(), exerciseId, 1,
                Kg.ofKg("60"), WeightUnit.KG, LoadMode.TOTAL, 1,
                Reps.of(4), Rir.of(0), SetType.WORKING, null, NOW, null, null);

        Workout once = workout.addSet(set);
        Workout twice = once.addSet(set); // mismo id de cliente

        assertThat(twice.sets()).hasSize(1);
    }

    // --- Caso 11: fuente de percentiles ausente ---------------------------

    @Test
    void sinFuenteNoSeInventanPuntos() {
        PercentileInterpolator.Interpolation missing = interpolator.interpolate(null, Kg.ofKg("68"));
        assertThat(missing.status()).isEqualTo(AssessmentStatus.SIN_FUENTE);
        assertThat(missing.percentile()).isEmpty();

        BenchmarkTable notLicensed = new BenchmarkTable(
                UUID.randomUUID(), UUID.randomUUID(), "TEST", "v1",
                BenchmarkCategory.of(Sex.MALE), null,
                false, false, List.of(BenchmarkPoint.of(50, "90")));
        PercentileInterpolator.Interpolation unusable =
                interpolator.interpolate(notLicensed, Kg.ofKg("90"));
        assertThat(unusable.status()).isEqualTo(AssessmentStatus.SIN_FUENTE);
    }

    // --- Caso 12: e1RM inicial 100, final 105 -> mejora 5% ----------------

    @Test
    void mejoraDelCincoPorCientoEsCandidata() {
        ImprovementCalculator calculator = new ImprovementCalculator(
                new BigDecimal("5.0"), new BigDecimal("3.0"), true);

        ImprovementCalculator.Result result = calculator.between(
                E1RM.of(Kg.ofKg("100")), E1RM.of(Kg.ofKg("105")), true);

        assertThat(result.percent()).isEqualByComparingTo("5.00");
        assertThat(result.candidate()).isTrue();
        // el delta (5 kg) no supera el doble del error asumido (6 kg): aun no confirmada
        assertThat(result.confirmed()).isFalse();
    }

    // --- Helpers ----------------------------------------------------------

    private WorkoutSet set(LoadMode mode, String load, int implementCount, int reps, int rir) {
        return new WorkoutSet(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                Kg.ofKg(load), WeightUnit.KG, mode, implementCount,
                Reps.of(reps), Rir.of(rir), SetType.WORKING, null, NOW, null, null);
    }

    private BenchmarkTable table(List<BenchmarkPoint> points) {
        return new BenchmarkTable(
                UUID.randomUUID(), UUID.randomUUID(), "TEST", "v1",
                BenchmarkCategory.of(Sex.MALE), null,
                true, false, points);
    }
}
