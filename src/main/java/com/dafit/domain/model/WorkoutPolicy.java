package com.dafit.domain.model;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Regla de negocio: una sesion vacia no es un entrenamiento (R-05).
 *
 * <p>El XP, la racha y las insignias se calculan sobre lo que el usuario hizo en
 * el gimnasio. Una sesion sin ninguna serie de trabajo con carga movilizada no
 * cuenta: si se premia, se sube de nivel abriendo y cerrando sesiones vacias.
 *
 * <p>"Carga movilizada" incluye el peso corporal (una dominada a peso corporal
 * registra 0 kg externos y sigue siendo una serie valida), por eso se prueba la
 * carga total y no la carga externa.
 */
public final class WorkoutPolicy {

    /** Minimo de series de trabajo con carga movilizada para contar como sesion. */
    public static final int MIN_WORKING_SETS = 1;

    private WorkoutPolicy() {
    }

    public static boolean isCountableWorkout(List<WorkoutSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return false;
        }
        long working = sets.stream()
                .filter(set -> set.type() == SetType.WORKING)
                .filter(WorkoutPolicy::movesLoad)
                .count();
        return working >= MIN_WORKING_SETS;
    }

    /** La carga total del peso corporal necesita datos coherentes: un fallo cuenta como "no". */
    private static boolean movesLoad(WorkoutSet set) {
        try {
            return set.totalLoad().isPositive();
        } catch (RuntimeException inconsistentLoad) {
            return false;
        }
    }

    /** Serie de trabajo con mas repeticiones validas: metrica oficial del peso corporal (C-08). */
    public static Optional<WorkoutSet> bestByReps(List<WorkoutSet> sets) {
        return sets.stream()
                .filter(set -> set.type() == SetType.WORKING)
                .filter(set -> set.reps().isOfficialRange())
                .filter(set -> set.rir().isOfficialRange())
                .max(Comparator.comparingInt(set -> set.reps().value()));
    }
}
