package com.dafit.domain.port.out;

import com.dafit.domain.model.MuscleCoefficient;
import com.dafit.domain.model.MuscleGroup;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

/** Puerto de salida: repositorio de coeficientes musculares. */
public interface MuscleCoefficientRepository {
    Optional<MuscleCoefficient> findById(UUID id);
    MuscleCoefficient save(MuscleCoefficient coefficient);
    boolean existsById(UUID id);
    List<MuscleCoefficient> findByVersion(String version);
    List<MuscleCoefficient> findByMuscleGroup(MuscleGroup muscle);
    List<MuscleCoefficient> findApproved();
    List<MuscleCoefficient> findEffective();

    default List<MuscleCoefficient> saveAll(Collection<MuscleCoefficient> coefficients) {
        return coefficients.stream().map(this::save).toList();
    }
}