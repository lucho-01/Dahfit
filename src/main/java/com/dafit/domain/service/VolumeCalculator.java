package com.dafit.domain.service;

import com.dafit.domain.model.Kg;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.WorkoutSet;

import java.util.List;

/**
 * Volumen de entrenamiento: suma de carga total por repeticiones.
 *
 * <p>Solo cuenta series de trabajo. El calentamiento no es volumen: incluirlo
 * infla las graficas y hace que el progreso parezca mayor de lo que es.
 */
public class VolumeCalculator {

    public Kg totalVolume(List<WorkoutSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return Kg.ZERO;
        }
        return sets.stream()
                .filter(set -> set.type() == SetType.WORKING)
                .map(set -> set.totalLoad().times(set.reps().value()))
                .reduce(Kg.ZERO, Kg::plus);
    }

    public Kg volumeFor(List<WorkoutSet> sets, java.util.UUID exerciseId) {
        return totalVolume(sets.stream()
                .filter(set -> set.exerciseId().equals(exerciseId))
                .toList());
    }
}
