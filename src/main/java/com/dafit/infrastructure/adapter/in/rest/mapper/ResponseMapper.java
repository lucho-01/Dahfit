package com.dafit.infrastructure.adapter.in.rest.mapper;

import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.model.Exercise;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.Routine;
import com.dafit.domain.model.RoutineExercise;
import com.dafit.domain.model.User;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutSet;
import com.dafit.domain.port.in.GetAssessmentSummaryUseCase;
import com.dafit.domain.port.in.GetGamificationUseCase;
import com.dafit.domain.port.in.LoginUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.response.AssessmentSummaryResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.BodyMetricResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.ExerciseAssessmentResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.ExerciseResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.GamificationResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.GlobalAssessmentResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.LoginResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.MuscleAssessmentResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.RoutineResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.SyncBatchResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.UserResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.WorkoutResponse;
import com.dafit.infrastructure.adapter.in.rest.dto.response.WorkoutSetResponse;

import java.util.List;

/**
 * Modelo de dominio -> DTO de salida.
 *
 * <p>Clase de utilidad sin estado ni Spring. El {@code Optional} del dominio se
 * convierte en {@code null}: {@code non_null} de Jackson omite los campos
 * ausentes y el campo {@code status} explica por que.
 */
public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static UserResponse user(User user) {
        return new UserResponse(
                user.id(),
                user.displayName(),
                user.role().name(),
                user.sex().name(),
                user.birthDate(),
                user.bodyweightKg() == null ? null : user.bodyweightKg().value(),
                user.preferredUnit().name(),
                user.timeZone().getId(),
                user.benchmarkConsent(),
                user.createdAt());
    }

    public static BodyMetricResponse bodyMetric(BodyMetric metric) {
        return new BodyMetricResponse(
                metric.id(),
                metric.userId(),
                metric.weightKg().value(),
                metric.date(),
                metric.recordedAt(),
                metric.source());
    }

    public static ExerciseResponse exercise(Exercise exercise) {
        List<String> muscles = exercise.muscles().stream()
                .map(Enum::name)
                .sorted()
                .toList();
        return new ExerciseResponse(
                exercise.id(),
                exercise.name(),
                exercise.equipment().name(),
                exercise.loadMode().name(),
                exercise.unilateral(),
                muscles,
                exercise.evaluable(),
                exercise.bodyweightFactor(),
                exercise.active());
    }

    public static ExerciseAssessmentResponse exerciseAssessment(ExerciseAssessment assessment) {
        return new ExerciseAssessmentResponse(
                assessment.id(),
                assessment.userId(),
                assessment.exerciseId(),
                assessment.status().name(),
                assessment.points().map(Points::value).orElse(null),
                assessment.rank().map(Enum::name).orElse(null),
                assessment.e1rm().map(e1rm -> e1rm.value().value()).orElse(null),
                assessment.relativeStrength().map(s -> s.value()).orElse(null),
                assessment.percentile().map(p -> p.value()).orElse(null),
                assessment.algorithmVersion().name(),
                assessment.benchmarkTableId(),
                assessment.bodyweightUsed().map(kg -> kg.value()).orElse(null),
                assessment.bestSetId(),
                assessment.assessedAt());
    }

    public static MuscleAssessmentResponse muscleAssessment(MuscleAssessment assessment) {
        return new MuscleAssessmentResponse(
                assessment.id(),
                assessment.userId(),
                assessment.muscle().name(),
                assessment.status().name(),
                assessment.points().map(Points::value).orElse(null),
                assessment.rank().map(Enum::name).orElse(null),
                assessment.coverage(),
                assessment.exercisesEvaluated(),
                assessment.primaryTestsEvaluated(),
                assessment.assessedAt());
    }

    public static GlobalAssessmentResponse globalAssessment(GlobalAssessment assessment) {
        return new GlobalAssessmentResponse(
                assessment.id(),
                assessment.userId(),
                assessment.status().name(),
                assessment.points().map(Points::value).orElse(null),
                assessment.rank().map(Enum::name).orElse(null),
                assessment.musclesEvaluated(),
                assessment.musclesRequired(),
                assessment.coveragePercent(),
                assessment.assessedAt());
    }

    public static AssessmentSummaryResponse assessmentSummary(GetAssessmentSummaryUseCase.Summary summary) {
        return new AssessmentSummaryResponse(
                summary.exercises().stream()
                        .map(ResponseMapper::exerciseAssessment)
                        .toList(),
                summary.muscles().stream()
                        .map(ResponseMapper::muscleAssessment)
                        .toList(),
                summary.global() == null ? null : globalAssessment(summary.global()));
    }

    public static WorkoutResponse workout(Workout workout) {
        List<WorkoutSetResponse> sets = workout.sets().stream()
                .map(ResponseMapper::workoutSet)
                .toList();
        return new WorkoutResponse(
                workout.id(),
                workout.userId(),
                workout.localDate(),
                workout.timeZone().getId(),
                workout.startedAt(),
                workout.finishedAt(),
                workout.status().name(),
                sets,
                workout.notes(),
                workout.totalVolume().value());
    }

    public static WorkoutSetResponse workoutSet(WorkoutSet set) {
        return new WorkoutSetResponse(
                set.id(),
                set.exerciseId(),
                set.order(),
                set.load().value(),
                set.inputUnit().name(),
                set.loadMode().name(),
                set.implementCount(),
                set.reps().value(),
                set.rir().value(),
                set.type().name(),
                set.notes(),
                set.performedAt(),
                set.totalLoad().value(),
                set.bodyweightKg() == null ? null : set.bodyweightKg().value(),
                set.bodyweightFactor());
    }

    public static RoutineResponse routine(Routine routine) {
        List<RoutineResponse.ExerciseSpecResponse> exercises = routine.orderedExercises().stream()
                .map(ResponseMapper::routineExercise)
                .toList();
        return new RoutineResponse(
                routine.id(),
                routine.ownerId(),
                routine.assignedToId(),
                routine.name(),
                exercises,
                routine.createdAt());
    }

    public static GamificationResponse gamification(GetGamificationUseCase.Summary summary) {
        List<GamificationResponse.BadgeResponse> badges = summary.badges().stream()
                .map(badge -> new GamificationResponse.BadgeResponse(
                        badge.badge().name(), badge.badge().label(), badge.earnedAt()))
                .toList();
        List<GamificationResponse.XpEventResponse> events = summary.recentEvents().stream()
                .map(event -> new GamificationResponse.XpEventResponse(
                        event.id(), event.type().name(), event.points(),
                        event.localDate(), event.occurredAt()))
                .toList();
        return new GamificationResponse(
                summary.totalXp(),
                summary.level().level(),
                summary.level().xpIntoLevel(),
                summary.level().xpForNextLevel(),
                summary.streak().currentDays(),
                summary.streak().longestDays(),
                summary.streak().lastActivityDate(),
                badges,
                events);
    }

    private static RoutineResponse.ExerciseSpecResponse routineExercise(RoutineExercise exercise) {
        return new RoutineResponse.ExerciseSpecResponse(
                exercise.exerciseId(),
                exercise.order(),
                exercise.targetSets(),
                exercise.targetReps().value(),
                exercise.restSeconds());
    }

    public static SyncBatchResponse syncBatch(List<com.dafit.domain.model.SyncResult> results) {
        List<SyncBatchResponse.SyncItemResponse> items = results.stream()
                .map(result -> new SyncBatchResponse.SyncItemResponse(
                        result.key(), result.target().name(), result.outcome().name(), result.message()))
                .toList();
        return SyncBatchResponse.of(items);
    }

    public static LoginResponse login(LoginUseCase.Result result) {
        return new LoginResponse(
                result.token(), result.tokenType(), result.expiresAt(), result.userId());
    }
}
