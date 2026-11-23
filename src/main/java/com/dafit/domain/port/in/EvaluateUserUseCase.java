package com.dafit.domain.port.in;

import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleGroup;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Puerto de entrada para recalcular y evaluar la fuerza completa de un usuario. */
public interface EvaluateUserUseCase {

    MuscleStep evaluateMuscle(UUID actorId, UUID userId, MuscleGroup muscle,
                              Instant assessedAt, boolean refreshSets);

    UserStep evaluateAll(UUID actorId, UUID userId, Instant assessedAt, boolean refreshSets);

    record MuscleStep(MuscleAssessment assessment, List<UUID> recalculated) {
        public MuscleStep {
            recalculated = recalculated == null ? List.of() : List.copyOf(recalculated);
        }
    }

    record UserStep(Map<MuscleGroup, MuscleStep> muscles, GlobalAssessment global) {
        public UserStep {
            muscles = muscles == null ? Map.of() : Map.copyOf(muscles);
        }
    }
}
