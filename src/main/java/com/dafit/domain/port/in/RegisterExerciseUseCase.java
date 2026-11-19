package com.dafit.domain.port.in;

import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.MuscleGroup;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/** Puerto de entrada: dar de alta un ejercicio en el catalogo (§4). */
public interface RegisterExerciseUseCase {

    UUID register(Command command);

    record Command(
            String name,
            Equipment equipment,
            LoadMode loadMode,
            boolean unilateral,
            Set<MuscleGroup> muscles,
            boolean evaluable,
            BigDecimal bodyweightFactor
    ) {
    }
}
