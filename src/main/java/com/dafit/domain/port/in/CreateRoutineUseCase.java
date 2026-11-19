package com.dafit.domain.port.in;

import com.dafit.domain.model.Reps;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: crear, duplicar y asignar rutinas (§5). */
public interface CreateRoutineUseCase {

    UUID create(Command command);

    UUID duplicate(Command command);

    UUID assign(Command command);

    /**
     * Comando comun a las tres operaciones.
     *
     * @param actorId    usuario autenticado ({@code null} con el API abierto)
     * @param ownerId    destinatario de la rutina nueva ({@code create})
     * @param name       nombre de la rutina ({@code create})
     * @param exercises  ejercicios ordenados ({@code create})
     * @param routineId  rutina de origen ({@code duplicate} y {@code assign})
     * @param newOwnerId nuevo propietario ({@code duplicate})
     * @param newName    nombre de la copia ({@code duplicate})
     * @param athleteId  atleta al que se asigna ({@code assign})
     */
    record Command(
            UUID actorId,
            UUID ownerId,
            String name,
            List<ExerciseSpec> exercises,
            UUID routineId,
            UUID newOwnerId,
            String newName,
            UUID athleteId
    ) {

        public static Command create(UUID actorId, UUID ownerId, String name, List<ExerciseSpec> exercises) {
            return new Command(actorId, ownerId, name, exercises, null, null, null, null);
        }

        public static Command duplicate(UUID actorId, UUID routineId, UUID newOwnerId, String newName) {
            return new Command(actorId, null, null, null, routineId, newOwnerId, newName, null);
        }

        public static Command assign(UUID actorId, UUID routineId, UUID athleteId) {
            return new Command(actorId, null, null, null, routineId, null, null, athleteId);
        }
    }

    record ExerciseSpec(UUID exerciseId, int targetSets, Reps targetReps, Integer restSeconds) {
    }
}
