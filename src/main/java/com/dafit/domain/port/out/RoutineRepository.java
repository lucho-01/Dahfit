package com.dafit.domain.port.out;

import com.dafit.domain.model.Routine;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de rutinas. */
public interface RoutineRepository {
    Optional<Routine> findById(UUID id);
    Routine save(Routine routine);
    boolean existsById(UUID id);
    List<Routine> findByOwnerId(UUID ownerId);
    List<Routine> findAll();
    List<Routine> findOwnedBy(UUID userId);
    List<Routine> findAssignedTo(UUID userId);
}