package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleCoefficient;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.PrimaryTest;
import com.dafit.domain.port.in.EvaluateMuscleUseCase;
import com.dafit.domain.port.out.ExerciseAssessmentRepository;
import com.dafit.domain.port.out.MuscleAssessmentRepository;
import com.dafit.domain.port.out.MuscleCoefficientRepository;
import com.dafit.domain.port.out.PrimaryTestRepository;
import com.dafit.domain.service.MuscleScoreCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso: evaluar un grupo muscular.
 *
 * <p>El denominador de la cobertura son TODOS los ejercicios con coeficiente
 * aprobado del musculo (R-01); los evaluados solo forman el numerador.
 */
public class EvaluateMuscleService implements EvaluateMuscleUseCase {

    private final MuscleScoreCalculator calculator;
    private final ExerciseAssessmentRepository exerciseAssessments;
    private final MuscleAssessmentRepository muscleAssessments;
    private final MuscleCoefficientRepository coefficients;
    private final PrimaryTestRepository primaryTests;
    private final AuthorizationSupport authorization;

    public EvaluateMuscleService(MuscleScoreCalculator calculator,
                                 ExerciseAssessmentRepository exerciseAssessments,
                                 MuscleAssessmentRepository muscleAssessments,
                                 MuscleCoefficientRepository coefficients,
                                 PrimaryTestRepository primaryTests,
                                 AuthorizationSupport authorization) {
        this.calculator = calculator;
        this.exerciseAssessments = exerciseAssessments;
        this.muscleAssessments = muscleAssessments;
        this.coefficients = coefficients;
        this.primaryTests = primaryTests;
        this.authorization = authorization;
    }

    @Override
    public MuscleAssessment evaluate(UUID actorId, UUID userId, MuscleGroup muscle, Instant assessedAt) {
        authorization.checkSameUser(actorId, userId);
        Instant at = assessedAt == null ? Instant.now() : assessedAt;

        List<ExerciseAssessment> evaluatedExercises = exerciseAssessments.findLatestForUser(userId)
                .map(List::of)
                .orElse(List.of());

        Map<UUID, BigDecimal> coefficientsForMuscle = coefficients.findApproved().stream()
                .filter(MuscleCoefficient::isEffective)
                .filter(c -> c.muscle() == muscle)
                .collect(Collectors.toMap(
                        MuscleCoefficient::exerciseId,
                        MuscleCoefficient::coefficient,
                        (first, second) -> first));

        Set<UUID> primaryExerciseIds = primaryTests.findAll().stream()
                .filter(t -> t.muscle() == muscle)
                .filter(PrimaryTest::primary)
                .map(PrimaryTest::exerciseId)
                .collect(Collectors.toSet());

        MuscleAssessment assessment = calculator.calculate(
                userId, muscle, evaluatedExercises, coefficientsForMuscle, primaryExerciseIds, at);

        return muscleAssessments.save(assessment);
    }
}
