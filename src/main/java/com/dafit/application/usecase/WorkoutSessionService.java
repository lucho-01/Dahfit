package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.PrimaryTest;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.User;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutPolicy;
import com.dafit.domain.model.WorkoutSet;
import com.dafit.domain.port.in.WorkoutSessionUseCase;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.domain.port.out.PrimaryTestRepository;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.domain.port.out.WorkoutRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Caso de uso: abrir, registrar series y cerrar una sesion.
 *
 * <ul>
 *   <li><b>R-03:</b> solo el duenio de la sesion puede anotar o cerrarla.</li>
 *   <li><b>R-07:</b> el ejercicio debe existir en el catalogo antes de guardar
 *       la serie (en PostgreSQL la clave foranea lo rechazaria con un 500).</li>
 *   <li><b>C-08:</b> las series de peso corporal resuelven su peso desde el dia
 *       de la pesada si el cliente no lo envia, y su factor desde el catalogo.</li>
 * </ul>
 */
public class WorkoutSessionService implements WorkoutSessionUseCase {

    private final WorkoutRepository workouts;
    private final UserRepository users;
    private final ExerciseRepository exercises;
    private final PrimaryTestRepository primaryTests;
    private final AuthorizationSupport authorization;

    public WorkoutSessionService(WorkoutRepository workouts,
                                 UserRepository users,
                                 ExerciseRepository exercises,
                                 PrimaryTestRepository primaryTests,
                                 AuthorizationSupport authorization) {
        this.workouts = workouts;
        this.users = users;
        this.exercises = exercises;
        this.primaryTests = primaryTests;
        this.authorization = authorization;
    }

    @Override
    public UUID start(StartCommand command) {
        authorization.checkSameUser(command.actorId(), command.userId());

        User user = users.findById(command.userId())
                .orElseThrow(() -> new DomainException.NotFound(
                        "usuario no encontrado: " + command.userId()));

        // Una sola sesion abierta por usuario: evita registros partidos en dos
        // sesiones simultaneas, que rompen el volumen y la racha.
        workouts.findInProgress(user.id()).ifPresent(open -> {
            throw new DomainException.Validation(
                    "ya hay una sesion abierta (" + open.id() + "); cierrala antes de abrir otra");
        });

        ZoneId zone = User.resolveTimeZone(user.timeZone(),
                command.timeZone() == null ? null : command.timeZone().getId());
        LocalDate localDate = command.localDate() == null
                ? LocalDate.now(zone)
                : command.localDate();

        Kg bodyweight = command.bodyweightKg() != null
                ? command.bodyweightKg()
                : user.bodyweightKg();

        Workout workout = Workout.start(user.id(), localDate, zone, Instant.now())
                .withBodyweight(bodyweight);
        return workouts.save(workout).id();
    }

    @Override
    public Workout logSet(SetCommand command) {
        Workout workout = workouts.findById(command.workoutId())
                .orElseThrow(() -> new DomainException.NotFound("sesion no encontrada"));
        authorization.checkSameUser(command.actorId(), workout.userId());

        if (!workout.isInProgress()) {
            throw new DomainException.Validation("no se pueden registrar series en una sesion cerrada");
        }

        // C-17: el id de la serie viene del dispositivo. Si ya existe, no se
        // duplica: el reenvio de una sincronizacion es un no-op.
        if (workout.findSet(command.clientUuid()).isPresent()) {
            return workout;
        }

        var exercise = exercises.findById(command.exerciseId())
                .orElseThrow(() -> new DomainException.NotFound(
                        "ejercicio no encontrado: " + command.exerciseId()));

        LoadMode loadMode = command.loadMode() == null ? exercise.loadMode() : command.loadMode();
        Kg bodyweight = command.bodyweightKg() != null ? command.bodyweightKg() : workout.bodyweight();
        Kg load = command.load();
        if (loadMode.isBodyweightBased() && bodyweight != null && (load == null || load.value().signum() < 0)) {
            load = Kg.ZERO;
        }

        WorkoutSet set = new WorkoutSet(
                command.clientUuid(),
                workout.id(),
                exercise.id(),
                workout.sets().size() + 1,
                load,
                command.inputUnit(),
                loadMode,
                command.implementCount(),
                command.reps(),
                command.rir(),
                command.type(),
                command.notes(),
                command.performedAt() == null ? Instant.now() : command.performedAt(),
                bodyweight,
                command.bodyweightFactor() != null ? command.bodyweightFactor() : exercise.bodyweightFactor());

        return workouts.save(workout.addSet(set));
    }

    @Override
    public Workout finish(FinishCommand command) {
        Workout workout = workouts.findById(command.workoutId())
                .orElseThrow(() -> new DomainException.NotFound("sesion no encontrada"));
        authorization.checkSameUser(command.actorId(), workout.userId());
        return workouts.save(workout.finish(Instant.now()));
    }

    /** Ejercicios con prueba primaria declarada del musculo: util para mensajes de cobertura. */
    boolean isPrimaryTestOf(UUID exerciseId, com.dafit.domain.model.MuscleGroup muscle) {
        return primaryTests.findAll().stream()
                .filter(test -> test.muscle() == muscle)
                .filter(PrimaryTest::primary)
                .anyMatch(test -> test.exerciseId().equals(exerciseId));
    }

    /** La sesion cuenta para XP si movilizo carga en al menos una serie de trabajo (R-05). */
    static boolean countsAsTraining(Workout workout) {
        return WorkoutPolicy.isCountableWorkout(workout.sets());
    }
}
