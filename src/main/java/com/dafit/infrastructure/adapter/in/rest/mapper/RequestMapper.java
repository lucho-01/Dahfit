package com.dafit.infrastructure.adapter.in.rest.mapper;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.SyncBatch;
import com.dafit.domain.model.SyncItem;
import com.dafit.domain.model.SyncTarget;
import com.dafit.domain.model.UserRole;
import com.dafit.domain.model.WeightUnit;
import com.dafit.domain.port.in.CreateRoutineUseCase;
import com.dafit.domain.port.in.EvaluateExerciseUseCase;
import com.dafit.domain.port.in.LoginUseCase;
import com.dafit.domain.port.in.RecordBodyMetricUseCase;
import com.dafit.domain.port.in.RegisterCredentialsUseCase;
import com.dafit.domain.port.in.RegisterExerciseUseCase;
import com.dafit.domain.port.in.RegisterUserUseCase;
import com.dafit.domain.port.in.WorkoutSessionUseCase;
import com.dafit.infrastructure.adapter.in.rest.dto.request.CreateRoutineRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.EvaluateExerciseRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.LogSetRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RecordBodyMetricRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RegisterCredentialsRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RegisterExerciseRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.RegisterUserRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.StartWorkoutRequest;
import com.dafit.infrastructure.adapter.in.rest.dto.request.SyncBatchRequest;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO de entrada -> comando del puerto de entrada.
 *
 * <p>Clase de utilidad sin estado. La conversion de enum es permisiva con los
 * valores ausentes (el dominio decide los valores por defecto) y estricta con
 * los presentes (un valor desconocido es error de contrato, no silencio).
 */
public final class RequestMapper {

    private RequestMapper() {
    }

    public static RegisterUserUseCase.Command toCommand(RegisterUserRequest request, boolean selfSignup) {
        return new RegisterUserUseCase.Command(
                request.displayName(),
                parseEnum(UserRole.class, request.role()),
                parseNullableEnum(Sex.class, request.sex()),
                request.birthDate(),
                kg(request.bodyweightKg()),
                parseNullableEnum(WeightUnit.class, request.preferredUnit()),
                request.timeZone() == null || request.timeZone().isBlank()
                        ? null : ZoneId.of(request.timeZone()),
                request.benchmarkConsent(),
                selfSignup);
    }

    public static RecordBodyMetricUseCase.Command toCommand(
            UUID actorId, UUID userId, RecordBodyMetricRequest request) {
        return new RecordBodyMetricUseCase.Command(
                actorId, userId, kg(request.weightKg()), request.date());
    }

    public static RegisterExerciseUseCase.Command toCommand(RegisterExerciseRequest request) {
        return new RegisterExerciseUseCase.Command(
                request.name(),
                parseEnum(Equipment.class, request.equipment()),
                parseEnum(LoadMode.class, request.loadMode()),
                request.unilateral(),
                request.muscles().stream()
                        .map(m -> parseEnum(MuscleGroup.class, m))
                        .collect(Collectors.toSet()),
                request.evaluable(),
                request.bodyweightFactor());
    }

    public static EvaluateExerciseUseCase.Command toCommand(
            UUID actorId, UUID userId, UUID exerciseId, EvaluateExerciseRequest request) {

        // R-04: ni el sexo ni la categoria se aceptan del cliente; el servicio
        // los deriva del perfil. Con bestSetId la carga la pone el servidor.
        return new EvaluateExerciseUseCase.Command(
                actorId,
                userId,
                exerciseId,
                request.bestSetId(),
                kg(request.totalLoadKg()),
                request.reps() == null ? null : Reps.of(request.reps()),
                request.rir() == null ? null : Rir.of(request.rir()),
                request.algorithmVersion() == null ? null : AlgorithmVersion.valueOf(request.algorithmVersion()),
                request.confirmOutlier(),
                request.assessedAt());
    }

    public static WorkoutSessionUseCase.StartCommand toCommand(UUID actorId, StartWorkoutRequest request) {
        return new WorkoutSessionUseCase.StartCommand(
                actorId,
                request.userId(),
                request.localDate(),
                request.timeZone() == null || request.timeZone().isBlank()
                        ? null : ZoneId.of(request.timeZone()),
                kg(request.bodyweightKg()));
    }

    public static WorkoutSessionUseCase.SetCommand toCommand(
            UUID actorId, UUID workoutId, LogSetRequest request) {
        return new WorkoutSessionUseCase.SetCommand(
                actorId,
                workoutId,
                request.clientUuid(),
                request.exerciseId(),
                kg(request.loadKg()),
                parseNullableEnum(WeightUnit.class, request.inputUnit()),
                parseEnum(LoadMode.class, request.loadMode()),
                request.implementCount(),
                Reps.of(request.reps()),
                Rir.of(request.rir()),
                request.type() == null ? null : SetType.valueOf(request.type()),
                request.notes(),
                request.performedAt(),
                kg(request.bodyweightKg()),
                request.bodyweightFactor());
    }

    public static CreateRoutineUseCase.Command toCommand(UUID actorId, CreateRoutineRequest request) {
        List<CreateRoutineUseCase.ExerciseSpec> specs = request.exercises().stream()
                .map(spec -> new CreateRoutineUseCase.ExerciseSpec(
                        spec.exerciseId(), spec.targetSets(), Reps.of(spec.targetReps()), spec.restSeconds()))
                .toList();
        return CreateRoutineUseCase.Command.create(actorId, request.ownerId(), request.name(), specs);
    }

    public static CreateRoutineUseCase.Command toDuplicateCommand(
            UUID actorId, UUID routineId, UUID newOwnerId, String newName) {
        return CreateRoutineUseCase.Command.duplicate(actorId, routineId, newOwnerId, newName);
    }

    public static CreateRoutineUseCase.Command toAssignCommand(
            UUID actorId, UUID routineId, UUID athleteId) {
        return CreateRoutineUseCase.Command.assign(actorId, routineId, athleteId);
    }

    public static SyncBatch toSyncBatch(SyncBatchRequest request) {
        List<SyncItem> items = request.items().stream()
                .map(RequestMapper::toSyncItem)
                .toList();
        return new SyncBatch(request.userId(), items);
    }

    private static SyncItem toSyncItem(SyncBatchRequest.SyncItemRequest item) {
        SyncTarget target = parseEnum(SyncTarget.class, item.target());
        return switch (target) {
            case WORKOUT_SET -> new SyncItem.WorkoutSetItem(
                    required(item.clientUuid(), "clientUuid"),
                    required(item.workoutId(), "workoutId"),
                    required(item.exerciseId(), "exerciseId"),
                    item.loadKg() == null ? null : Kg.ofKg(item.loadKg()),
                    parseNullableEnum(WeightUnit.class, item.inputUnit()),
                    parseNullableEnum(LoadMode.class, item.loadMode()),
                    item.implementCount() == null ? 1 : item.implementCount(),
                    Reps.of(required(item.reps(), "reps")),
                    // R-06: un RIR ausente NO se inventa como 0; el caso de uso
                    // rechaza el item con su propio mensaje.
                    item.rir() == null ? null : Rir.of(item.rir()),
                    parseNullableEnum(SetType.class, item.type()),
                    item.notes(),
                    item.performedAt());
            case WORKOUT_FINISH -> new SyncItem.WorkoutFinishItem(
                    required(item.workoutId(), "workoutId"),
                    item.finishedAt());
        };
    }

    public static RegisterCredentialsUseCase.Command toCommand(
            UUID actorId, UUID userId, RegisterCredentialsRequest request) {
        return new RegisterCredentialsUseCase.Command(
                actorId, userId, request.email(), request.password(), request.claimCode());
    }

    public static LoginUseCase.Command toCommand(LoginRequest request) {
        return new LoginUseCase.Command(request.email(), request.password(), request.claimCode());
    }

    private static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("campo obligatorio ausente: " + field);
        }
        return value;
    }

    private static Kg kg(BigDecimal value) {
        return value == null ? null : Kg.ofKg(value);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("valor obligatorio ausente para " + type.getSimpleName());
        }
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    private static <E extends Enum<E>> E parseNullableEnum(Class<E> type, String value) {
        return value == null || value.isBlank()
                ? null
                : Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }
}
