package com.dafit.application.usecase;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.SyncBatch;
import com.dafit.domain.model.SyncItem;
import com.dafit.domain.model.SyncResult;
import com.dafit.domain.model.Workout;
import com.dafit.domain.port.in.AwardWorkoutXpUseCase;
import com.dafit.domain.port.in.SyncBatchUseCase;
import com.dafit.domain.port.in.WorkoutSessionUseCase;
import com.dafit.domain.port.out.WorkoutRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: aplicar un lote offline en orden, sin abortar por un item malo.
 *
 * <p>Cada item se resuelve de forma independiente: un cierre de sesion no puede
 * tumbar el lote entero ni impedir que se guarden las series anteriores. El
 * resultado le dice al cliente que borrar de la cola (APPLIED/DUPLICATE) y que
 * corregir (REJECTED).
 *
 * <p><b>R-06:</b> cualquier fallo de un item, sea de dominio o de integridad
 * (dato que la base rechazaria), se convierte en {@code REJECTED} con su
 * mensaje. Antes, un {@code loadMode} ausente provocaba un
 * {@code NullPointerException} sin capturar y todo el lote respondia 500.
 *
 * <p><b>R-03:</b> con un actor autenticado, el lote solo puede ser del propio
 * usuario.
 */
public class SyncBatchService implements SyncBatchUseCase {

    private final WorkoutRepository workouts;
    private final WorkoutSessionUseCase session;
    private final AwardWorkoutXpUseCase awardXp;
    private final AuthorizationSupport authorization;

    public SyncBatchService(WorkoutRepository workouts,
                            WorkoutSessionUseCase session,
                            AwardWorkoutXpUseCase awardXp,
                            AuthorizationSupport authorization) {
        this.workouts = workouts;
        this.session = session;
        this.awardXp = awardXp;
        this.authorization = authorization;
    }

    @Override
    public List<SyncResult> process(UUID actorId, SyncBatch batch) {
        authorization.checkSameUser(actorId, batch.userId());

        List<SyncResult> results = new ArrayList<>(batch.items().size());
        for (SyncItem item : batch.items()) {
            results.add(apply(actorId, batch.userId(), item));
        }
        return List.copyOf(results);
    }

    private SyncResult apply(UUID actorId, UUID userId, SyncItem item) {
        try {
            Workout workout = workouts.findById(item.workoutId())
                    .orElseThrow(() -> new DomainException.NotFound(
                            "sesion no encontrada: " + item.workoutId()));

            if (!workout.userId().equals(userId)) {
                return SyncResult.rejected(item.key(), item.target(),
                        "la sesion no pertenece al usuario");
            }

            return switch (item) {
                case SyncItem.WorkoutSetItem set -> applySet(actorId, workout, set);
                case SyncItem.WorkoutFinishItem finish -> applyFinish(actorId, workout, finish);
            };
        } catch (RuntimeException ex) {
            // R-06: un item mal formado o en conflicto con la base no tumba el lote.
            return SyncResult.rejected(item.key(), item.target(), messageOf(ex));
        }
    }

    private SyncResult applySet(UUID actorId, Workout workout, SyncItem.WorkoutSetItem item) {
        // C-17: si la serie ya existe es un duplicado aunque la sesion ya este
        // cerrada; asi un reintento viejo no genera un falso error.
        if (workout.findSet(item.clientUuid()).isPresent()) {
            return SyncResult.duplicate(item.key(), item.target());
        }
        if (!workout.isInProgress()) {
            return SyncResult.rejected(item.key(), item.target(),
                    "no se pueden registrar series en una sesion cerrada");
        }
        if (item.performedAt() != null && !item.performedAt().isAfter(workout.startedAt())) {
            return SyncResult.rejected(item.key(), item.target(),
                    "la serie es anterior al inicio de la sesion");
        }
        // R-06: el RIR no se puede inferir. Antes se guardaba 0 (una afirmacion
        // falsa sobre el esfuerzo); ahora el item se rechaza y el lote sigue.
        if (item.rir() == null) {
            return SyncResult.rejected(item.key(), item.target(),
                    "falta el RIR de la serie");
        }

        session.logSet(new WorkoutSessionUseCase.SetCommand(
                actorId,
                item.workoutId(),
                item.clientUuid(),
                item.exerciseId(),
                item.load(),
                item.inputUnit(),
                item.loadMode(),
                item.implementCount(),
                item.reps(),
                item.rir(),
                item.type(),
                item.notes(),
                item.performedAt(),
                null,
                null));

        return SyncResult.applied(item.key(), item.target());
    }

    private SyncResult applyFinish(UUID actorId, Workout workout, SyncItem.WorkoutFinishItem item) {
        if (workout.status() == com.dafit.domain.model.WorkoutStatus.COMPLETED) {
            return SyncResult.duplicate(item.key(), item.target());
        }

        Instant at = item.finishedAt() == null ? Instant.now() : item.finishedAt();
        if (at.isBefore(workout.startedAt())) {
            return SyncResult.rejected(item.key(), item.target(),
                    "el cierre es anterior al inicio de la sesion");
        }

        Workout finished = session.finish(new WorkoutSessionUseCase.FinishCommand(actorId, workout.id()));
        awardXp.onWorkoutCompleted(finished, at);

        return SyncResult.applied(item.key(), item.target());
    }

    private static String messageOf(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "no se pudo aplicar el item" : message;
    }
}
