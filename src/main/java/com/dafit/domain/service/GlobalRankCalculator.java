package com.dafit.domain.service;

import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Assessment;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.RankScale;
import com.dafit.domain.model.Points;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rango general (§7.2, §7.4).
 *
 * <p><b>Regla dura:</b> con {@code requireAllMuscles = true} (valor por defecto),
 * si falta un solo musculo NO hay rango global. Se devuelve el estado
 * {@code COBERTURA_INSUFICIENTE} con el contador, y la interfaz lo muestra como
 * progreso de cobertura, nunca como un rango.
 *
 * <p>Promediar 7 musculos y llamarlo "rango general" es exactamente el error que
 * la especificacion prohibe y que este servicio hace imposible por construccion.
 */
public class GlobalRankCalculator {

    private final RankScale rankScale;
    private final int requiredMuscles;
    private final boolean requireAllMuscles;

    public GlobalRankCalculator(RankScale rankScale, int requiredMuscles, boolean requireAllMuscles) {
        this.rankScale = rankScale;
        this.requiredMuscles = requiredMuscles;
        this.requireAllMuscles = requireAllMuscles;
    }

    public GlobalAssessment calculate(UUID userId, List<MuscleAssessment> muscles, Instant assessedAt) {

        Map<MuscleGroup, Points> official = new EnumMap<>(MuscleGroup.class);

        for (MuscleAssessment assessment : muscles) {
            if (!assessment.isOfficial() || assessment.points().isEmpty()) {
                continue;
            }
            // Si un musculo llega duplicado, se conserva el primero: la lista
            // viene del repositorio y ya esta ordenada por fecha de corte.
            official.putIfAbsent(assessment.muscle(), assessment.points().get());
        }

        int evaluated = official.size();

        if (evaluated == 0) {
            return GlobalAssessment.incomplete(userId, 0, requiredMuscles, assessedAt);
        }

        if (requireAllMuscles && evaluated < requiredMuscles) {
            return GlobalAssessment.incomplete(userId, evaluated, requiredMuscles, assessedAt);
        }

        Points average = Points.average(official.values());

        return GlobalAssessment.official(
                userId, average, rankScale.classify(average), requiredMuscles, assessedAt);
    }

    public int requiredMuscles() {
        return requiredMuscles;
    }

    public boolean requireAllMuscles() {
        return requireAllMuscles;
    }
}
