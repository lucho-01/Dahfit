package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.port.in.EvaluateGlobalUseCase;
import com.dafit.domain.port.out.GlobalAssessmentRepository;
import com.dafit.domain.port.out.MuscleAssessmentRepository;
import com.dafit.domain.service.GlobalRankCalculator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Caso de uso: evaluar el rango general. */
public class EvaluateGlobalService implements EvaluateGlobalUseCase {

    private final GlobalRankCalculator calculator;
    private final MuscleAssessmentRepository muscleAssessments;
    private final GlobalAssessmentRepository globalAssessments;
    private final AuthorizationSupport authorization;

    public EvaluateGlobalService(GlobalRankCalculator calculator,
                                 MuscleAssessmentRepository muscleAssessments,
                                 GlobalAssessmentRepository globalAssessments,
                                 AuthorizationSupport authorization) {
        this.calculator = calculator;
        this.muscleAssessments = muscleAssessments;
        this.globalAssessments = globalAssessments;
        this.authorization = authorization;
    }

    @Override
    public GlobalAssessment evaluate(UUID actorId, UUID userId, Instant assessedAt) {
        authorization.checkSameUser(actorId, userId);
        Instant at = assessedAt == null ? Instant.now() : assessedAt;

        List<MuscleAssessment> muscles = muscleAssessments.findLatestForUser(userId);
        GlobalAssessment assessment = calculator.calculate(userId, muscles, at);

        return globalAssessments.save(assessment);
    }
}
