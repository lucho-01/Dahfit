package com.dafit.domain.service;

import com.dafit.domain.model.Badge;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Decide que insignias nuevas corresponden al usuario.
 *
 * <p>Los criterios son deliberadamente simples y auditables: se calculan con
 * datos que ya existen (sesiones completadas y record de racha), no con
 * eventos opacos. Una insignia nunca se retira.
 */
public class BadgeEvaluator {

    public static final int STREAK_DAYS_FOR_BADGE = 4;
    public static final int WORKOUTS_FOR_TEN_BADGE = 10;

    public record Stats(int completedWorkouts, int longestStreakDays) {

        public Stats {
            if (completedWorkouts < 0 || longestStreakDays < 0) {
                throw new IllegalArgumentException("las estadisticas no pueden ser negativas");
            }
        }
    }

    public Set<Badge> newBadges(Stats stats, Set<Badge> alreadyOwned) {
        Objects.requireNonNull(stats, "las estadisticas son obligatorias");
        Objects.requireNonNull(alreadyOwned, "las insignias actuales son obligatorias");

        EnumSet<Badge> earned = EnumSet.noneOf(Badge.class);
        if (stats.completedWorkouts() >= 1) {
            earned.add(Badge.PRIMER_ENTRENAMIENTO);
        }
        if (stats.longestStreakDays() >= STREAK_DAYS_FOR_BADGE) {
            earned.add(Badge.RACHA_4);
        }
        if (stats.completedWorkouts() >= WORKOUTS_FOR_TEN_BADGE) {
            earned.add(Badge.DIEZ_ENTRENAMIENTOS);
        }
        earned.removeAll(alreadyOwned);
        return earned;
    }
}
