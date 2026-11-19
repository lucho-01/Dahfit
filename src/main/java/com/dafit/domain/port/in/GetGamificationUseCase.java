package com.dafit.domain.port.in;

import com.dafit.domain.model.Streak;
import com.dafit.domain.model.UserBadge;
import com.dafit.domain.model.XpEvent;
import com.dafit.domain.model.XpLevel;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: resumen de gamificacion del usuario (C-19). */
public interface GetGamificationUseCase {

    Summary summaryFor(UUID actorId, UUID userId);

    /**
     * @param totalXp      XP acumulado historico
     * @param recentEvents ultimos eventos, mas reciente primero
     */
    record Summary(
            int totalXp,
            XpLevel level,
            Streak streak,
            List<UserBadge> badges,
            List<XpEvent> recentEvents
    ) {

        public Summary {
            badges = List.copyOf(badges);
            recentEvents = List.copyOf(recentEvents);
        }
    }
}
