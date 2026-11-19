package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleCoefficient;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.User;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutSet;
import com.dafit.domain.port.in.EvaluateExerciseUseCase;
import com.dafit.domain.port.in.EvaluateGlobalUseCase;
import com.dafit.domain.port.in.EvaluateMuscleUseCase;
import com.dafit.domain.port.out.MuscleCoefficientRepository;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.domain.port.out.WorkoutRepository;
import com.dafit.domain.service.BestSetSelector;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: evaluar la fuerza de un usuario de punta a punta (§5, §7).
 *
 * <p><b>Por que existe (R-15):</b> antes el cliente tenia que enviar a mano la
 * carga, las repeticiones y el RIR de cada ejercicio para pedir su rango, lo que
 * convertia al dispositivo en la fuente de verdad de la fuerza. Ahora el
 * servidor toma las series ya registradas, elige la mejor por e1RM
 * ({@link BestSetSelector}) y las evalua con el mismo caso de uso que usa el
 * endpoint individual.
 *
 * <p>La evaluacion de un ejercicio puede acabar en estados parciales
 * (SIN_FUENTE, FALTA_PESO_CORPORAL...): eso NO es un error. El musculo se
 * calcula despues con {@code MuscleScoreCalculator}, que solo cuenta
 * evaluaciones oficiales.
 */
public class EvaluateUserService {

    /** Sesiones recientes donde se busca la mejor serie de cada ejercicio. */
    static final int LOOKBACK_WORKOUTS = 120;

    private final EvaluateExerciseUseCase exerciseUseCase;
    private final EvaluateMuscleUseCase muscleEvaluator;
    private final EvaluateGlobalUseCase globalEvaluator;
    private final WorkoutRepository workouts;
    private final MuscleCoefficientRepository coefficients;
    private final UserRepository users;
    private final BestSetSelector bestSets;
    private final AuthorizationSupport authorization;
    private final AlgorithmVersion defaultAlgorithm;

    public EvaluateUserService(EvaluateExerciseUseCase exerciseUseCase,
                               EvaluateMuscleUseCase muscleEvaluator,
                               EvaluateGlobalUseCase globalEvaluator,
                               WorkoutRepository workouts,
                               MuscleCoefficientRepository coefficients,
                               UserRepository users,
                               BestSetSelector bestSets,
                               AuthorizationSupport authorization,
                               AlgorithmVersion defaultAlgorithm) {
        this.exerciseUseCase = exerciseUseCase;
        this.muscleEvaluator = muscleEvaluator;
        this.globalEvaluator = globalEvaluator;
        this.workouts = workouts;
        this.coefficients = coefficients;
        this.users = users;
        this.bestSets = bestSets;
        this.authorization = authorization;
        this.defaultAlgorithm = defaultAlgorithm;
    }

    /**
     * Recalcula las series del musculo y devuelve su evaluacion.
     *
     * @param refreshSets si {@code false} se usa el historial de evaluaciones tal cual
     */
    public MuscleStep evaluateMuscle(UUID actorId, UUID userId, MuscleGroup muscle,
                                     Instant assessedAt, boolean refreshSets) {
        authorization.checkSameUser(actorId, userId);
        User user = requireBenchmarkUser(userId);
        Instant at = assessedAt == null ? Instant.now() : assessedAt;

        List<UUID> recalculated = refreshSets
                ? refresh(user, exercisesOf(muscle), actorId, userId, at)
                : List.of();

        MuscleAssessment assessment = muscleEvaluator.evaluate(actorId, userId, muscle, at);
        return new MuscleStep(assessment, recalculated);
    }

    /**
     * Evalua los ocho musculos y compone el rango general.
     *
     * @param refreshSets si {@code true} recalcula antes las series de todo el catalogo
     */
    public UserStep evaluateAll(UUID actorId, UUID userId, Instant assessedAt, boolean refreshSets) {
        authorization.checkSameUser(actorId, userId);
        User user = requireBenchmarkUser(userId);
        Instant at = assessedAt == null ? Instant.now() : assessedAt;

        if (refreshSets) {
            refresh(user, allEvaluableExerciseIds(), actorId, userId, at);
        }

        Map<MuscleGroup, MuscleStep> byMuscle = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup muscle : MuscleGroup.values()) {
            byMuscle.put(muscle, new MuscleStep(
                    muscleEvaluator.evaluate(actorId, userId, muscle, at), List.of()));
        }

        GlobalAssessment global = globalEvaluator.evaluate(actorId, userId, at);
        return new UserStep(byMuscle, global);
    }

    // --- datos --------------------------------------------------------------

    private List<UUID> refresh(User user, Set<UUID> exerciseIds, UUID actorId, UUID userId, Instant at) {
        Map<UUID, WorkoutSet> best = bestSetsFor(user, exerciseIds, at);
        List<UUID> evaluated = new ArrayList<>(best.size());
        for (Map.Entry<UUID, WorkoutSet> entry : best.entrySet()) {
            exerciseUseCase.evaluate(new EvaluateExerciseUseCase.Command(
                    actorId, userId, entry.getKey(), entry.getValue().id(),
                    null, null, null, defaultAlgorithm, false, at));
            evaluated.add(entry.getKey());
        }
        return evaluated;
    }

    private User requireBenchmarkUser(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new DomainException.NotFound("usuario no encontrado: " + userId));
        if (!user.canPublishBenchmarks()) {
            throw new DomainException.Forbidden(
                    "para evaluar la fuerza hacen falta sexo declarado y consentimiento de benchmark");
        }
        return user;
    }

    private Set<UUID> exercisesOf(MuscleGroup muscle) {
        return approvedCoefficients().stream()
                .filter(coefficient -> coefficient.muscle() == muscle)
                .map(MuscleCoefficient::exerciseId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<UUID> allEvaluableExerciseIds() {
        return approvedCoefficients().stream()
                .map(MuscleCoefficient::exerciseId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<MuscleCoefficient> approvedCoefficients() {
        return coefficients.findApproved().stream()
                .filter(MuscleCoefficient::isEffective)
                .toList();
    }

    /**
     * Mejor serie por ejercicio en las sesiones cerradas del ultimo ano.
     *
     * <p>No se miran sesiones abiertas (el usuario aun puede corregirlas) ni
     * series que no son de trabajo.
     */
    private Map<UUID, WorkoutSet> bestSetsFor(User user, Set<UUID> exerciseIds, Instant at) {
        Map<UUID, List<WorkoutSet>> byExercise = new LinkedHashMap<>();
        LocalDate oldest = LocalDate.ofInstant(at, user.timeZone()).minusYears(1);

        for (Workout workout : workouts.findRecent(user.id(), LOOKBACK_WORKOUTS)) {
            if (workout.isInProgress() || workout.localDate().isBefore(oldest)) {
                continue;
            }
            for (WorkoutSet set : workout.sets()) {
                if (set.type() != SetType.WORKING || !exerciseIds.contains(set.exerciseId())) {
                    continue;
                }
                byExercise.computeIfAbsent(set.exerciseId(), key -> new ArrayList<>()).add(set);
            }
        }

        Map<UUID, WorkoutSet> best = new LinkedHashMap<>();
        byExercise.forEach((exerciseId, sets) -> bestSets.selectBest(sets, defaultAlgorithm)
                .ifPresent(set -> best.put(exerciseId, set)));
        return best;
    }

    /**
     * Resultado del recalculo de un musculo.
     *
     * @param assessment   evaluacion del musculo persistida
     * @param recalculated ejercicios reevaluados desde su historial
     */
    public record MuscleStep(MuscleAssessment assessment, List<UUID> recalculated) {
        public MuscleStep {
            recalculated = recalculated == null ? List.of() : List.copyOf(recalculated);
        }
    }

    /** Resultado completo: los ocho musculos y el rango general. */
    public record UserStep(Map<MuscleGroup, MuscleStep> muscles, GlobalAssessment global) {
        public UserStep {
            muscles = muscles == null ? Map.of() : Map.copyOf(muscles);
        }
    }
}