package com.dafit.domain.service;

import com.dafit.domain.service.E1RmStrategy;
import com.dafit.domain.service.E1RmStrategyRegistry;
import com.dafit.domain.model.WorkoutSet;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Selecciona la mejor serie de un ejercicio (§7.1).
 *
 * <p><b>Regla dura:</b> la mejor serie es la de mayor e1RM, NO la de mayor peso.
 * Una serie de 50 kg x 10 puede valer mas que una de 60 kg x 4, y es la primera
 * la que debe alimentar la evaluacion.
 */
public class BestSetSelector {

    private final E1RmStrategyRegistry strategies;

    public BestSetSelector(E1RmStrategyRegistry strategies) {
        this.strategies = strategies;
    }

    public Optional<WorkoutSet> selectBest(List<WorkoutSet> sets,
                                           com.dafit.domain.model.AlgorithmVersion version) {
        if (sets == null || sets.isEmpty()) {
            return Optional.empty();
        }
        E1RmStrategy strategy = strategies.get(version);

        return sets.stream()
                .filter(WorkoutSet::isEligibleForOfficialEstimation)
                .max(Comparator.comparing(set -> strategy.estimate(set.totalLoad(), set.reps()).value()));
    }

    public boolean isHighConfidence(WorkoutSet set) {
        return set != null && set.isHighConfidence();
    }
}
