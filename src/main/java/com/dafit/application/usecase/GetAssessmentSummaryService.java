package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.port.in.GetAssessmentSummaryUseCase;
import com.dafit.domain.port.out.ExerciseAssessmentRepository;
import com.dafit.domain.port.out.GlobalAssessmentRepository;
import com.dafit.domain.port.out.MuscleAssessmentRepository;

import java.util.List;
import java.util.UUID;

/** Caso de uso: ultimo estado de evaluacion del usuario. */
public class GetAssessmentSummaryService implements GetAssessmentSummaryUseCase {

    private final ExerciseAssessmentRepository exerciseAssessments;
    private final MuscleAssessmentRepository muscleAssessments;
    private final GlobalAssessmentRepository globalAssessments;
    private final AuthorizationSupport authorization;

    public GetAssessmentSummaryService(ExerciseAssessmentRepository exerciseAssessments,
                                       MuscleAssessmentRepository muscleAssessments,
                                       GlobalAssessmentRepository globalAssessments,
                                       AuthorizationSupport authorization) {
        this.exerciseAssessments = exerciseAssessments;
        this.muscleAssessments = muscleAssessments;
        this.globalAssessments = globalAssessments;
        this.authorization = authorization;
    }

    @Override
    public Summary summaryFor(UUID actorId, UUID userId) {
        authorization.checkSameUser(actorId, userId);

        // Cada repositorio devuelve ya solo la ultima evaluacion por ejercicio y
        // por musculo; el historico completo es detalle de auditoria, no de UI.
        List<ExerciseAssessment> exercises = exerciseAssessments.findLatestForUser(userId)
                .map(List::of)
                .orElse(List.of());
        List<MuscleAssessment> muscles = muscleAssessments.findLatestForUser(userId)
                .map(List::of)
                .orElse(List.of());
        GlobalAssessment global = globalAssessments.findLatestFor(userId).orElse(null);

        return new Summary(exercises, muscles, global);
    }
}
