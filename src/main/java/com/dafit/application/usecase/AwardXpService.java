package com.dafit.application.usecase;

import com.dafit.domain.model.Badge;
import com.dafit.domain.model.Streak;
import com.dafit.domain.model.UserBadge;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutPolicy;
import com.dafit.domain.model.XpEvent;
import com.dafit.domain.model.XpEventType;
import com.dafit.domain.model.XpGrant;
import com.dafit.domain.port.in.AwardWorkoutXpUseCase;
import com.dafit.domain.port.out.BadgeRepository;
import com.dafit.domain.port.out.StreakRepository;
import com.dafit.domain.port.out.WorkoutRepository;
import com.dafit.domain.port.out.XpEventRepository;
import com.dafit.domain.service.BadgeEvaluator;
import com.dafit.domain.service.XpCalculator;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: XP, racha e insignias al cerrar una sesion.
 *
 * <p>Se invoca despues de persistir la sesion, nunca dentro del dominio de
 * entrenamiento. Si algo falla aqui, la sesion ya quedo guardada: el XP es
 * recuperable y no debe bloquear el registro del entrenamiento.
 *
 * <p><b>R-05:</b> una sesion vacia no otorga XP, no mueve la racha y no da
 * insignias.
 *
 * <p><b>R-09:</b> el calculo completo (eventos, racha e insignias) se valida
 * ANTES de escribir nada, para que un fallo a mitad no deje un estado parcial
 * que la idempotencia por {@code sourceKey} impediria reparar.
 */
public class AwardXpService implements AwardWorkoutXpUseCase {

    private final XpEventRepository xpEvents;
    private final StreakRepository streaks;
    private final BadgeRepository badges;
    private final WorkoutRepository workouts;
    private final XpCalculator calculator;
    private final BadgeEvaluator badgeEvaluator;
    private final int sessionXp;
    private final int firstSessionOfWeekXp;

    public AwardXpService(XpEventRepository xpEvents,
                          StreakRepository streaks,
                          BadgeRepository badges,
                          WorkoutRepository workouts,
                          XpCalculator calculator,
                          BadgeEvaluator badgeEvaluator,
                          int sessionXp,
                          int firstSessionOfWeekXp) {
        this.xpEvents = xpEvents;
        this.streaks = streaks;
        this.badges = badges;
        this.workouts = workouts;
        this.calculator = calculator;
        this.badgeEvaluator = badgeEvaluator;
        this.sessionXp = sessionXp;
        this.firstSessionOfWeekXp = firstSessionOfWeekXp;
    }

    @Override
    public Update onWorkoutCompleted(Workout workout, Instant at) {
        UUID userId = workout.userId();
        Instant effectiveAt = at == null ? Instant.now() : at;
        LocalDate localDate = workout.localDate() == null
                ? LocalDate.ofInstant(effectiveAt, workout.timeZone())
                : workout.localDate();

        Streak current = streaks.findByUserId(userId).orElseGet(() -> Streak.empty(userId));

        if (!WorkoutPolicy.isCountableWorkout(workout.sets())) {
            return new Update(List.of(), current, List.of());
        }

        String sourceKey = "workout:" + workout.id();
        if (xpEvents.existsBySourceKey(userId, sourceKey)) {
            return new Update(List.of(), current, List.of());
        }

        // 1. Calcular TODO antes de escribir (R-09)
        LocalDate weekStart = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<XpEvent> weekEvents = xpEvents.findBetween(userId, weekStart, weekStart.plusDays(6));

        List<XpGrant> proposals = new ArrayList<>();
        proposals.add(new XpGrant(XpEventType.SESSION_COMPLETED, sessionXp, localDate, sourceKey));

        boolean firstOfWeek = weekEvents.stream()
                .noneMatch(event -> event.type() == XpEventType.SESSION_COMPLETED);
        if (firstOfWeek) {
            proposals.add(new XpGrant(
                    XpEventType.FIRST_SESSION_OF_WEEK,
                    firstSessionOfWeekXp,
                    localDate,
                    "first-week:" + weekStart));
        }

        List<XpGrant> accepted = calculator.apply(userId, proposals, weekEvents);
        Streak updated = current.register(localDate);

        Set<Badge> newBadges = badgeEvaluator.newBadges(
                new BadgeEvaluator.Stats(
                        (int) workouts.countCompleted(userId),
                        updated.longestDays()),
                badges.badgesOf(userId));

        // 2. Escribir (cada guardado es su propia transaccion del adaptador;
        //    en produccion el caso de uso se declara @Transactional).
        List<XpEvent> awarded = accepted.stream()
                .map(grant -> xpEvents.save(toEvent(userId, grant, effectiveAt)))
                .toList();

        if (!updated.equals(current)) {
            streaks.save(updated);
        }

        List<Badge> earned = new ArrayList<>();
        for (Badge badge : newBadges) {
            badges.save(userId, badge);
            earned.add(badge);
        }

        return new Update(awarded, updated, earned);
    }

    private static XpEvent toEvent(UUID userId, XpGrant grant, Instant at) {
        return new XpEvent(
                UUID.randomUUID(), userId, grant.type(), grant.points(),
                grant.localDate(), at, grant.sourceKey());
    }
}
