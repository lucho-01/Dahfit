package com.dafit.domain.port.in;

import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.Exercise;
import com.dafit.domain.model.MuscleGroup;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Puerto de entrada: consultar el catalogo de ejercicios (§5). */
public interface ListExercisesUseCase {

    List<Exercise> list(Filter filter);

    /**
     * @throws com.dafit.domain.exception.DomainException.NotFound si no existe
     */
    Exercise byId(UUID exerciseId);

    record Filter(Set<MuscleGroup> muscles, Equipment equipment, boolean onlyEvaluable) {

        public static Filter all() {
            return new Filter(Set.of(), null, false);
        }
    }
}
