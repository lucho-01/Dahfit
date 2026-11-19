package com.dafit.domain.port.in;

import com.dafit.domain.model.Routine;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: consultar rutinas (§5). */
public interface GetRoutinesUseCase {

    /**
     * @throws com.dafit.domain.exception.DomainException.NotFound si no existe
     */
    Routine byId(UUID actorId, UUID routineId);

    /** Rutinas que el usuario posee o tiene asignadas, sin duplicados y de mas reciente a mas antigua. */
    List<Routine> forUser(UUID actorId, UUID userId);
}
