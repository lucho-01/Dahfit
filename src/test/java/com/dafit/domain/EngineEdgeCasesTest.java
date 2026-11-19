package com.dafit.domain;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.BenchmarkPoint;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.RankScale;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.StrengthRank;
import com.dafit.domain.model.WeightUnit;
import com.dafit.domain.model.WorkoutSet;
import com.dafit.domain.service.EpleyV1;
import com.dafit.domain.service.PercentileInterpolator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Casos de borde del motor, complementarios a la §11.
 *
 * <p>Cada uno cubre un limite donde el redondeo o la validacion tienden a
 * fallar silenciosamente. No son inventados: son las reglas del documento
 * (§7.1, §7.2, §7.3) probadas justo en su frontera.
 */
class EngineEdgeCasesTest {

    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");

    private final EpleyV1 epley = new EpleyV1();
    private final PercentileInterpolator interpolator = new PercentileInterpolator();

    // --- C-01: una repeticion no infla el 3,33 % --------------------------

    @Test
    void unaRepeticionDevuelveLaCarga() {
        assertThat(epley.estimate(Kg.ofKg("60"), Reps.of(1)).value().value())
                .isEqualByComparingTo("60.0000");
    }

    // --- §7.3: cortes de rango exactos ------------------------------------

    @Test
    void cortePrincipianteNovato() {
        assertThat(RankScale.specV1().classify(Points.of(199))).isEqualTo(StrengthRank.PRINCIPIANTE);
        assertThat(RankScale.specV1().classify(Points.of(200))).isEqualTo(StrengthRank.NOVATO);
    }

    @Test
    void corteEliteBrutal() {
        assertThat(RankScale.specV1().classify(Points.of(949))).isEqualTo(StrengthRank.ELITE);
        assertThat(RankScale.specV1().classify(Points.of(950))).isEqualTo(StrengthRank.BRUTAL);
    }

    // --- §7.2: fuera de P5/P95 no se extrapola ----------------------------

    @Test
    void porDebajoDeP5NoSeExtrapola() {
        BenchmarkTable table = table(List.of(
                BenchmarkPoint.of(50, "90"),
                BenchmarkPoint.of(80, "130")));

        assertThat(interpolator.interpolate(table, Kg.ofKg("70")).status())
                .isEqualTo(AssessmentStatus.FUERA_DE_TABLA_P5);
    }

    @Test
    void porEncimaDeP95NoSeExtrapola() {
        BenchmarkTable table = table(List.of(
                BenchmarkPoint.of(50, "90"),
                BenchmarkPoint.of(80, "130")));

        assertThat(interpolator.interpolate(table, Kg.ofKg("150")).status())
                .isEqualTo(AssessmentStatus.FUERA_DE_TABLA_P95);
    }

    // --- §7.2: tabla no monotona se rechaza en la carga -------------------

    @Test
    void tablaNoMonotonaSeRechaza() {
        assertThatThrownBy(() -> new BenchmarkTable(
                UUID.randomUUID(), UUID.randomUUID(), "TEST", "v1",
                BenchmarkCategory.of(Sex.MALE), null, true, false,
                List.of(BenchmarkPoint.of(50, "90"), BenchmarkPoint.of(60, "80"))))
                .isInstanceOf(DomainException.Validation.class);
    }

    // --- C-04: RIR 5 se registra pero no es oficial -----------------------

    @Test
    void rirCincoNoEsElegible() {
        WorkoutSet set = new WorkoutSet(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                Kg.ofKg("60"), WeightUnit.KG, LoadMode.TOTAL, 1,
                Reps.of(5), Rir.of(5), SetType.WORKING, null, NOW, null, null);

        assertThat(set.isEligibleForOfficialEstimation()).isFalse();
        assertThat(set.rir().value()).isEqualTo(5); // se conserva el dato registrado
    }

    private BenchmarkTable table(List<BenchmarkPoint> points) {
        return new BenchmarkTable(
                UUID.randomUUID(), UUID.randomUUID(), "TEST", "v1",
                BenchmarkCategory.of(Sex.MALE), null, true, false, points);
    }
}
