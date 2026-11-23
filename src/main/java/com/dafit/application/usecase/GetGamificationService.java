package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.model.Streak;
import com.dafit.domain.model.UserBadge;
import com.dafit.domain.model.XpLevel;
import com.dafit.domain.port.in.GetGamificationUseCase;
import com.dafit.domain.port.out.BadgeRepository;
import com.dafit.domain.port.out.StreakRepository;
import com.dafit.domain.port.out.XpEventRepository;
import java.util.List;
import java.util.UUID;

/** Caso de uso: resumen de gamificacion para la pantalla de progreso. */
public class GetGamificationService implements GetGamificationUseCase {

    private static final int RECENT_EVENTS = 10;

    private final XpEventRepository xpEvents;
    private final StreakRepository streaks;
    private final BadgeRepository badges;
    private final AuthorizationSupport authorization;

    public GetGamificationService(XpEventRepository xpEvents,
                                  StreakRepository streaks,
                                  BadgeRepository badges,
                                  AuthorizationSupport authorization) {
        this.xpEvents = xpEvents;
        this.streaks = streaks;
        this.badges = badges;
        this.authorization = authorization;
    }

    @Override
    public Summary summaryFor(UUID actorId, UUID userId) {
        authorization.checkSameUser(actorId, userId);

        int totalXp = (int) xpEvents.totalPoints(userId);
        Streak streak = streaks.findByUserId(userId).orElseGet(() -> Streak.empty(userId));

        List<UserBadge> userBadges = badges.findByUserId(userId);
        return new Summary(
                totalXp,
                XpLevel.of(totalXp),
                streak,
                userBadges,
                xpEvents.findRecent(userId, RECENT_EVENTS));
    }
}
