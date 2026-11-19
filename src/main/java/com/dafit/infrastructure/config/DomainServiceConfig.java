package com.dafit.infrastructure.config;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.RankScale;
import com.dafit.domain.service.BestSetSelector;
import com.dafit.domain.service.BrzyckiV1;
import com.dafit.domain.service.E1RmStrategy;
import com.dafit.domain.service.E1RmStrategyRegistry;
import com.dafit.domain.service.EpleyV1;
import com.dafit.domain.service.GlobalRankCalculator;
import com.dafit.domain.service.ImprovementCalculator;
import com.dafit.domain.service.MaxPointsAggregation;
import com.dafit.domain.service.MuscleAggregationStrategy;
import com.dafit.domain.service.MuscleCoefficientResolver;
import com.dafit.domain.service.MuscleScoreCalculator;
import com.dafit.domain.service.OutlierDetector;
import com.dafit.domain.service.PercentileInterpolator;
import com.dafit.domain.service.VolumeCalculator;
import com.dafit.domain.service.WeightedMeanAggregation;
import com.dafit.domain.service.BadgeEvaluator;
import com.dafit.domain.service.XpCalculator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Locale;

/**
 * Registro de los servicios de dominio como beans de Spring.
 *
 * <p>El dominio no lleva {@code @Service}: son clases planas, probables sin
 * levantar contexto. Toda la dependencia de Spring queda en esta capa, que es
 * exactamente donde el hexagono permite tenerla.
 */
@Configuration
@EnableConfigurationProperties(DafitProperties.class)
public class DomainServiceConfig {

    /**
     * Escala de puntos a rango (§7.3).
     *
     * <p>{@code SPEC_V1} es la del documento. {@code RECALIBRATED_V1} existe
     * lista pero solo se activa cambiando {@code dafit.engine.rank-scale};
     * C-10 sigue pendiente de aprobacion.
     */
    @Bean
    public RankScale rankScale(DafitProperties properties) {
        String selected = normalized(properties.engine().rankScale(), "SPEC_V1");
        return switch (selected) {
            case "SPEC_V1" -> RankScale.specV1();
            case "RECALIBRATED_V1" -> RankScale.recalibratedV1();
            default -> throw new DomainException.Validation(
                    "escala de rangos desconocida: " + selected
                            + ". Valores validos: SPEC_V1, RECALIBRATED_V1");
        };
    }

    // --- Calculo de e1RM (Strategy versionado) ---------------------------

    @Bean
    public EpleyV1 epleyV1() {
        return new EpleyV1();
    }

    @Bean
    public BrzyckiV1 brzyckiV1() {
        return new BrzyckiV1();
    }

    @Bean
    public E1RmStrategyRegistry e1RmStrategyRegistry(List<E1RmStrategy> strategies) {
        return new E1RmStrategyRegistry(strategies);
    }

    @Bean
    public BestSetSelector bestSetSelector(E1RmStrategyRegistry strategies) {
        return new BestSetSelector(strategies);
    }

    @Bean
    public PercentileInterpolator percentileInterpolator() {
        return new PercentileInterpolator();
    }

    // --- Puntuacion muscular y rango (§7.4) ------------------------------

    @Bean
    public MuscleAggregationStrategy muscleAggregationStrategy(DafitProperties properties) {
        String selected = normalized(properties.assessment().muscleAggregation(), MaxPointsAggregation.ID);
        return switch (selected) {
            case MaxPointsAggregation.ID -> new MaxPointsAggregation();
            case WeightedMeanAggregation.ID -> new WeightedMeanAggregation();
            default -> throw new DomainException.Validation(
                    "agregacion muscular desconocida: " + selected
                            + ". Valores validos: " + MaxPointsAggregation.ID
                            + ", " + WeightedMeanAggregation.ID);
        };
    }

    @Bean
    public MuscleCoefficientResolver muscleCoefficientResolver() {
        return new MuscleCoefficientResolver();
    }

    @Bean
    public MuscleScoreCalculator muscleScoreCalculator(
            MuscleAggregationStrategy aggregation,
            RankScale rankScale,
            DafitProperties properties) {

        return new MuscleScoreCalculator(
                aggregation,
                rankScale,
                properties.assessment().minPrimaryTests(),
                properties.assessment().minCoverageRatio());
    }

    @Bean
    public GlobalRankCalculator globalRankCalculator(RankScale rankScale, DafitProperties properties) {
        return new GlobalRankCalculator(
                rankScale,
                MuscleGroup.values().length,
                properties.assessment().requireAllMusclesForGlobal());
    }

    // --- Progreso, outliers, volumen -------------------------------------

    @Bean
    public ImprovementCalculator improvementCalculator(DafitProperties properties) {
        return new ImprovementCalculator(
                properties.improvement().officialThresholdPercent(),
                properties.improvement().estimatorErrorPercent(),
                properties.improvement().requireHighConfidence());
    }

    @Bean
    public OutlierDetector outlierDetector(DafitProperties properties) {
        return new OutlierDetector(
                properties.outlier().maxMultipleOfHistory(),
                properties.outlier().maxE1rmJumpPercent());
    }

    @Bean
    public VolumeCalculator volumeCalculator() {
        return new VolumeCalculator();
    }

    // --- Gamificacion (C-19) ---------------------------------------------

    @Bean
    public XpCalculator xpCalculator(DafitProperties properties) {
        DafitProperties.Gamification gamification = properties.gamification();
        return new XpCalculator(
                gamification.dailyXpCap(),
                gamification.weeklyXpCap(),
                gamification.maxXpSessionsPerDay());
    }

    @Bean
    public BadgeEvaluator badgeEvaluator() {
        return new BadgeEvaluator();
    }

    private static String normalized(String value, String fallback) {
        String chosen = value == null || value.isBlank() ? fallback : value;
        return chosen.trim().toUpperCase(Locale.ROOT);
    }
}
