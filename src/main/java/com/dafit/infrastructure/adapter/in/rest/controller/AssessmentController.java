package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.port.in.EvaluateExerciseUseCase;
import com.dafit.domain.port.in.EvaluateGlobalUseCase;
import com.dafit.domain.port.in.EvaluateMuscleUseCase;
import com.dafit.domain.port.in.GetAssessmentSummaryUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.EvaluateExerciseRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.response.AssessmentSummaryResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.ExerciseAssessmentResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.GlobalAssessmentResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.MuscleAssessmentResponse;
import com.dafit.infrastructure.adapter.in.rest.mapper.RequestMapper;
import com.dafit.infrastructure.adapter.in.rest.mapper.ResponseMapper;
import com.dafit.infrastructure.adapter.in.rest.security.Actors;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** Evaluaciones de fuerza: ejercicio, musculo y rango general. */
@RestController
@RequestMapping("/api")
public class AssessmentController {

    private final EvaluateExerciseUseCase evaluateExercise;
    private final EvaluateMuscleUseCase evaluateMuscle;
    private final EvaluateGlobalUseCase evaluateGlobal;
    private final GetAssessmentSummaryUseCase summary;

    public AssessmentController(EvaluateExerciseUseCase evaluateExercise,
                                EvaluateMuscleUseCase evaluateMuscle,
                                EvaluateGlobalUseCase evaluateGlobal,
                                GetAssessmentSummaryUseCase summary) {
        this.evaluateExercise = evaluateExercise;
        this.evaluateMuscle = evaluateMuscle;
        this.evaluateGlobal = evaluateGlobal;
        this.summary = summary;
    }

    @PostMapping("/users/{userId}/exercises/{exerciseId}/assessments")
    public ExerciseAssessmentResponse assessExercise(
            Authentication authentication,
            @PathVariable UUID userId,
            @PathVariable UUID exerciseId,
            @Valid @RequestBody EvaluateExerciseRequest request) {

        return ResponseMapper.exerciseAssessment(evaluateExercise.evaluate(
                RequestMapper.toCommand(Actors.actorId(authentication), userId, exerciseId, request)));
    }

    @PostMapping("/users/{userId}/muscles/{muscle}/assessment")
    public MuscleAssessmentResponse assessMuscle(
            Authentication authentication,
            @PathVariable UUID userId,
            @PathVariable MuscleGroup muscle) {

        return ResponseMapper.muscleAssessment(evaluateMuscle.evaluate(
                Actors.actorId(authentication), userId, muscle, Instant.now()));
    }

    @PostMapping("/users/{userId}/global-assessment")
    public GlobalAssessmentResponse assessGlobal(Authentication authentication,
                                                 @PathVariable UUID userId) {
        return ResponseMapper.globalAssessment(evaluateGlobal.evaluate(
                Actors.actorId(authentication), userId, Instant.now()));
    }

    /**
     * Ultima evaluacion de cada ejercicio y de cada musculo, mas el ultimo rango
     * general. Es lo que pinta la pantalla de progreso sin traerse el historico.
     */
    @GetMapping("/users/{userId}/assessments")
    public AssessmentSummaryResponse summary(Authentication authentication,
                                             @PathVariable UUID userId) {
        return ResponseMapper.assessmentSummary(summary.summaryFor(
                Actors.actorId(authentication), userId));
    }
}
