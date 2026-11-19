package com.dafit.domain.port.in;

import com.dafit.domain.model.Badge;
import com.dafit.domain.model.Streak;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.XpEvent;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de entrada: otorgar el XP de una sesion completada.
 *
 * <p>Es idempotente por {@code workoutId}: cerrar o reenviar la misma sesion no
 * vuelve a dar XP ni a mover la racha.
 *
 * <p><b>R-05:</b> una sesion vacia (sin ninguna serie de trabajo con carga
 * movilizada) no otorga nada: ni XP, ni racha, ni insignias. La decision vive en
 * el dominio ({@code WorkoutPolicy}), no en el controlador.
 *
 * <p><b>C-18/R-05:</b> la fecha local de la sesion se usa tal cual; si una
 * sesion no tiene ninguna, se deriva del instante en la zona del usuario.
 */
public interface AwardWorkoutXpUseCase {

    Update onWorkoutCompleted(Workout workout, Instant at);

    /** Lo que cambio en el usuario con esta sesion. Listas vacias si fue no-op. */
    record Update(List<XpEvent> events, Streak streak, List<Badge> newBadges) {

        public Update {
            events = List.copyOf(events);
            newBadges = List.copyOf(newBadges);
        }

        public boolean isEmpty() {
            return events.isEmpty() && newBadges.isEmpty();
        }
    }
}
