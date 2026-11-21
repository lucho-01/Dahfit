package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.Exercise;
import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.E1RM;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Percentile;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.RankScale;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.User;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutSet;
import com.dafit.domain.port.in.EvaluateExerciseUseCase;
import com.dafit.domain.port.in.ResolveBodyweightUseCase;
import com.dafit.domain.port.out.BenchmarkTableRepository;
import com.dafit.domain.port.out.ExerciseAssessmentRepository;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.domain.port.out.WorkoutRepository;
import com.dafit.domain.service.E1RmStrategyRegistry;
import com.dafit.domain.service.OutlierDetector;
import com.dafit.domain.service.PercentileInterpolator;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: evaluar un ejercicio de principio a fin.
 *
 * <p><b>R-04:</b> la categoria de referencia se deriva del PERFIL (sexo, fecha
 * de nacimiento y consentimiento), no del cuerpo de la peticion. Antes cualquier
 * usuario elegia la categoria que le daba mejor percentil y se evaluaba sin
 * consentimiento.
 *
 * <p><b>R-15:</b> si llega un {@code bestSetId}, la serie se busca en el
 * historial del PROPIO usuario: un id ajeno o inexistente no se convierte en
 * "evaluacion" con datos que el cliente pudo inventar. La mejor serie de la
 * sesion la elige el servidor; el cliente solo puede pedir "la mejor de esta
 * sesion" mediante {@code workoutId}, y el calculo se hace con
 * {@code BestSetSelector} sobre series reales.
 *
 * <p><b>R-06 (C-16):</b> una serie implausible frente al historico no entra al
 * calculo oficial salvo que el cliente la confirme explicitamente.
 *
 * <p>Orden de comprobaciones, deliberado: primero el calculo puro, despues los
 * datos que pueden faltar. Asi una serie no elegible nunca produce un e1RM
 * fantasma y un dato ausente nunca produce un 0 disfrazado.
 */
public class EvaluateExerciseService implements EvaluateExerciseUseCase {

    /** Sesiones recientes que se recorren para localizar una serie (7 meses ~ 200 sesiones). */
    private static final int RECENT_WORKOUTS_FOR_LOOKUP = 200;

    private final E1RmStrategyRegistry strategies;
    private final PercentileInterpolator interpolator;
    private final RankScale rankScale;
    private final BenchmarkTableRepository benchmarks;
    private final ExerciseAssessmentRepository assessments;
    private final ResolveBodyweightUseCase bodyweights;
    private final OutlierDetector outliers;
    private final UserRepository users;
    private final ExerciseRepository exercises;
    private final WorkoutRepository workouts;
    private final AuthorizationSupport authorization;
    private final AlgorithmVersion defaultVersion;

    public EvaluateExerciseService(E1RmStrategyRegistry strategies,
                                   PercentileInterpolator interpolator,
                                   RankScale rankScale,
                                   BenchmarkTableRepository benchmarks,
                                   ExerciseAssessmentRepository assessments,
                                   ResolveBodyweightUseCase bodyweights,
                                   OutlierDetector outliers,
                                   UserRepository users,
                                   ExerciseRepository exercises,
                                   WorkoutRepository workouts,
                                   AuthorizationSupport authorization,
                                   AlgorithmVersion defaultVersion) {
        this.strategies = strategies;
        this.interpolator = interpolator;
        this.rankScale = rankScale;
        this.benchmarks = benchmarks;
        this.assessments = assessments;
        this.bodyweights = bodyweights;
        this.outliers = outliers;
        this.users = users;
        this.exercises = exercises;
        this.workouts = workouts;
        this.authorization = authorization;
        this.defaultVersion = defaultVersion;
    }

    @Override
    public ExerciseAssessment evaluate(Command command) {
        UUID userId = command.userId();
        UUID exerciseId = command.exerciseId();
        authorization.checkSameUser(command.actorId(), userId);

        User user = users.findById(userId)
                .orElseThrow(() -> new DomainException.NotFound("usuario no encontrado: " + userId));
        exercises.findById(exerciseId)
                .orElseThrow(() -> new DomainException.NotFound(
                        "ejercicio no encontrado: " + exerciseId));

        Exercise exercise = exercises.findById(exerciseId).get();

        // DEC-05: Ejercicio sin estándares registrados
        // Si el ejercicio no es evaluable (no tiene benchmark registrado),
        // retornar estado 'No disponible' en lugar de intentar calcular
        if (!exercise.evaluable()) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, AssessmentStatus.NO_DISPONIBLE,
                    Optional.empty(), null, defaultVersion, null, Instant.now()));
        }

        Instant at = command.assessedAt() == null ? Instant.now() : command.assessedAt();
        AlgorithmVersion version = command.algorithmVersion() == null
                ? defaultVersion
                : command.algorithmVersion();

        Optional<Attempt> attempt = resolveAttempt(command, version);
        if (attempt.isEmpty()) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, AssessmentStatus.SIN_DATOS,
                    Optional.empty(), null, version, command.bestSetId(), at));
        }
        Attempt source = attempt.get();

        // 1. Elegibilidad de la serie (§7.1)
        if (source.reps() == null || !source.reps().isOfficialRange()
                || source.rir() == null || !source.rir().isOfficialRange()
                || source.totalLoad() == null || !source.totalLoad().isPositive()) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, AssessmentStatus.SIN_DATOS,
                    Optional.empty(), null, version, source.bestSetId(), at));
        }

        // 2. e1RM con la version de algoritmo solicitada
        E1RM e1rm;
        try {
            e1rm = strategies.get(version).estimate(source.totalLoad(), source.reps());
        } catch (DomainException.NotEligible notEligible) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, AssessmentStatus.SIN_DATOS,
                    Optional.empty(), null, version, source.bestSetId(), at));
        }

        // 3. Carga atipica (C-16): no entra al calculo sin confirmacion explicita
        if (source.externalLoad() != null) {
            OutlierDetector.Verdict verdict = outliers.checkLoad(
                    source.externalLoad(), historicalMaxLoad(userId, exerciseId, source.bestSetId()));
            if (verdict.suspicious() && !command.confirmOutlier()) {
                return assessments.save(ExerciseAssessment.notEvaluated(
                        userId, exerciseId, AssessmentStatus.SIN_DATOS,
                        Optional.of(e1rm), null, version, source.bestSetId(), at));
            }
        }

        // 4. Peso corporal (C-05, R-11)
        Kg bodyweight;
        try {
            bodyweight = bodyweights.resolve(userId, at);
        } catch (DomainException.InsufficientData missingWeight) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, AssessmentStatus.FALTA_PESO_CORPORAL,
                    Optional.of(e1rm), null, version, source.bestSetId(), at));
        }

        // 5. Categoria de referencia derivada del perfil (R-04)
        Optional<BenchmarkCategory> category = categoryOf(user, at);
        if (category.isEmpty()) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, AssessmentStatus.FALTA_CATEGORIA,
                    Optional.of(e1rm), bodyweight, version, source.bestSetId(), at));
        }

        // 6. Tabla de referencia licenciada (§7.2, §9, R-16)
        Optional<BenchmarkTable> table = benchmarks.findFor(exerciseId, category.get(), bodyweight);
        if (table.isEmpty() || !table.get().isUsable()) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, AssessmentStatus.SIN_FUENTE,
                    Optional.of(e1rm), bodyweight, version, source.bestSetId(), at));
        }

        // 7. Interpolacion de percentil
        PercentileInterpolator.Interpolation interpolation =
                interpolator.interpolate(table.get(), e1rm.value());

        if (!interpolation.isOfficial()) {
            return assessments.save(ExerciseAssessment.notEvaluated(
                    userId, exerciseId, interpolation.status(),
                    Optional.of(e1rm), bodyweight, version, source.bestSetId(), at));
        }

        Percentile percentile = interpolation.percentile().orElseThrow();
        Points points = percentile.toPoints();

        // 8. Evaluacion oficial
        ExerciseAssessment assessment = ExerciseAssessment.evaluated(
                userId, exerciseId, e1rm, bodyweight, percentile,
                rankScale.classify(points), version, table.get().id(),
                source.bestSetId(), at);

        return assessments.save(assessment);
    }

    /**
     * Categoria de referencia del usuario (§4, R-04).
     *
     * <p>Exige consentimiento explicito ({@link User#canPublishBenchmarks()}) y
     * sexo declarado. La banda de edad se calcula con la fecha de nacimiento si
     * existe; las fuentes que no la publican usan la categoria sin banda.
     */
    public static Optional<BenchmarkCategory> categoryOf(User user, Instant at) {
        if (user == null || !user.canPublishBenchmarks()) {
            return Optional.empty();
        }
        return Optional.of(new BenchmarkCategory(user.sex(), AgeBands.labelFor(user.birthDate(), at)));
    }

    // --- origen del dato ---------------------------------------------------

    private Optional<Attempt> resolveAttempt(Command command, AlgorithmVersion version) {
        if (command.bestSetId() != null) {
            return findUserSet(command.userId(), command.bestSetId())
                    .filter(set -> set.exerciseId().equals(command.exerciseId()))
                    .flatMap(set -> fromSet(set));
        }
        if (command.totalLoad() == null) {
            return Optional.empty();
        }
        // Entrada manual: el usuario declara la carga total ya resuelta y no
        // hay serie real contra la que contrastar atipicos.
        return Optional.of(new Attempt(command.totalLoad(), command.reps(), command.rir(), null, null));
    }

    private Optional<Attempt> fromSet(WorkoutSet set) {
        try {
            return Optional.of(new Attempt(set.totalLoad(), set.reps(), set.rir(), set.load(), set.id()));
        } catch (DomainException inconsistentLoad) {
            // Serie de peso corporal sin peso corporal del dia: no resoluble.
            return Optional.empty();
        }
    }

    private Optional<WorkoutSet> findUserSet(UUID userId, UUID setId) {
        for (Workout workout : workouts.findRecent(userId, RECENT_WORKOUTS_FOR_LOOKUP)) {
            Optional<WorkoutSet> found = workout.findSet(setId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /**
     * Maximo historico de carga externa del ejercicio, excluyendo la serie evaluada.
     * Es la referencia del detector de atipicos (C-16).
     */
    private Optional<Kg> historicalMaxLoad(UUID userId, UUID exerciseId, UUID excludedSetId) {
        Kg max = null;
        for (Workout workout : workouts.findRecent(userId, RECENT_WORKOUTS_FOR_LOOKUP)) {
            for (WorkoutSet set : workout.setsFor(exerciseId)) {
                if (excludedSetId != null && excludedSetId.equals(set.id())) {
                    continue;
                }
                if (!set.isEligibleForOfficialEstimation()) {
                    continue;
                }
                if (max == null || set.load().compareTo(max) > 0) {
                    max = set.load();
                }
            }
        }
        return Optional.ofNullable(max);
    }

    /**
     * Datos normalizados de la serie a evaluar.
     *
     * @param externalLoad carga externa registrada (solo si viene del historial)
     */
    private record Attempt(Kg totalLoad, Reps reps, Rir rir, Kg externalLoad, UUID bestSetId) {
    }
}
