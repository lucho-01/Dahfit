package com.dafit.infrastructure.config;

import com.dafit.domain.model.AlgorithmVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Parametros del motor DAFIT, enlazados desde {@code application.yml}.
 *
 * <p>Se modelan como {@code record} inmutable: la configuracion se lee una vez
 * al arrancar y no cambia en caliente. Cada campo corresponde a una decision
 * documentada en {@code docs/DAHIAN_02_Correcciones_Propuestas.md}.
 *
 * <p>Las claves {@code dafit.gamification.*} alimentan el modulo de
 * gamificacion (C-19): valor de cada evento y topes diario/semanal.
 */
@ConfigurationProperties(prefix = "dafit")
public record DafitProperties(
        Engine engine,
        Assessment assessment,
        Improvement improvement,
        Outlier outlier,
        Gamification gamification,
        Security security
) {

    /** {@code dafit.engine.*} */
    public record Engine(
            AlgorithmVersion defaultAlgorithm,
            String rankScale
    ) {
    }

    /** {@code dafit.assessment.*} */
    public record Assessment(
            int bodyweightLookupDays,
            int officialRepsMin,
            int officialRepsMax,
            String muscleAggregation,
            int minPrimaryTests,
            BigDecimal minCoverageRatio,
            boolean requireAllMusclesForGlobal
    ) {
    }

    /** {@code dafit.improvement.*} - §7.5 y C-11. */
    public record Improvement(
            BigDecimal officialThresholdPercent,
            BigDecimal estimatorErrorPercent,
            boolean requireHighConfidence
    ) {
    }

    /** {@code dafit.outlier.*} - C-16. */
    public record Outlier(
            BigDecimal maxMultipleOfHistory,
            BigDecimal maxE1rmJumpPercent
    ) {
    }

    /** {@code dafit.gamification.*} - C-19. */
    public record Gamification(
            int dailyXpCap,
            int weeklyXpCap,
            int maxXpSessionsPerDay,
            int sessionCompletedXp,
            int firstSessionOfWeekXp,
            int fourWeekReviewXp
    ) {
    }

    /**
     * {@code dafit.security.*}.
     *
     * <p>{@code enabled=false} mantiene el API abierto mientras el frontend
     * integra el login; al pasar a true, todo salvo auth/salud/OpenAPI exige
     * Bearer token.
     */
    public record Security(
            boolean enabled,
            String jwtSecret,
            int jwtTtlMinutes
    ) {
    }
}
