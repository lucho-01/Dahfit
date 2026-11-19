package com.dafit.domain;

import com.dafit.domain.model.Badge;
import com.dafit.domain.model.Streak;
import com.dafit.domain.model.XpEvent;
import com.dafit.domain.model.XpEventType;
import com.dafit.domain.model.XpGrant;
import com.dafit.domain.model.XpLevel;
import com.dafit.domain.service.BadgeEvaluator;
import com.dafit.domain.service.XpCalculator;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Reglas de gamificacion de C-19: topes, idempotencia, racha e insignias. */
class GamificationTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private final XpCalculator calculator = new XpCalculator(200, 500, 2);

    @Test
    void elTopeDiarioRecortaElXpSobrante() {
        List<XpGrant> accepted = calculator.apply(USER, List.of(
                grant(XpEventType.SESSION_COMPLETED, 120, MONDAY, "a"),
                grant(XpEventType.SESSION_COMPLETED, 120, MONDAY, "b")), List.of());

        assertThat(accepted).extracting(XpGrant::points).containsExactly(120, 80);
    }

    @Test
    void soloDosSesionesDanXpPorDia() {
        List<XpGrant> accepted = calculator.apply(USER, List.of(
                grant(XpEventType.SESSION_COMPLETED, 50, MONDAY, "a"),
                grant(XpEventType.SESSION_COMPLETED, 50, MONDAY, "b"),
                grant(XpEventType.SESSION_COMPLETED, 50, MONDAY, "c")), List.of());

        assertThat(accepted).hasSize(2);
    }

    @Test
    void elTopeSemanalLimitaAunqueQuedeMargenDiario() {
        XpEvent previous = event(XpEventType.SESSION_COMPLETED, 480, MONDAY, "prev");

        List<XpGrant> accepted = calculator.apply(USER,
                List.of(grant(XpEventType.FIRST_SESSION_OF_WEEK, 50, MONDAY.plusDays(2), "bonus")),
                List.of(previous));

        assertThat(accepted).hasSize(1);
        assertThat(accepted.getFirst().points()).isEqualTo(20);
    }

    @Test
    void unaClaveYaOtorgadaNoVuelveADarXp() {
        XpEvent previous = event(XpEventType.SESSION_COMPLETED, 50, MONDAY, "workout:1");

        List<XpGrant> accepted = calculator.apply(USER,
                List.of(grant(XpEventType.SESSION_COMPLETED, 50, MONDAY, "workout:1")),
                List.of(previous));

        assertThat(accepted).isEmpty();
    }

    @Test
    void sinMargenNoSeCreaEvento() {
        XpEvent previous = event(XpEventType.SESSION_COMPLETED, 200, MONDAY, "prev");

        List<XpGrant> accepted = calculator.apply(USER,
                List.of(grant(XpEventType.SESSION_COMPLETED, 50, MONDAY, "nuevo")),
                List.of(previous));

        assertThat(accepted).isEmpty();
    }

    @Test
    void laRachaSumaDiasConsecutivosYConservaElRecord() {
        Streak streak = Streak.empty(USER)
                .register(MONDAY)
                .register(MONDAY.plusDays(1))
                .register(MONDAY.plusDays(1))
                .register(MONDAY.plusDays(3));

        assertThat(streak.currentDays()).isEqualTo(1);
        assertThat(streak.longestDays()).isEqualTo(2);
        assertThat(streak.lastActivityDate()).isEqualTo(MONDAY.plusDays(3));
    }

    @Test
    void unEventoAtrasadoNoReescribeLaRacha() {
        Streak streak = Streak.empty(USER)
                .register(MONDAY.plusDays(2))
                .register(MONDAY);

        assertThat(streak.currentDays()).isEqualTo(1);
        assertThat(streak.lastActivityDate()).isEqualTo(MONDAY.plusDays(2));
    }

    @Test
    void lasInsigniasNoSeRepiten() {
        BadgeEvaluator evaluator = new BadgeEvaluator();

        Set<Badge> first = evaluator.newBadges(
                new BadgeEvaluator.Stats(1, 1), Set.of());
        Set<Badge> second = evaluator.newBadges(
                new BadgeEvaluator.Stats(1, 1), first);

        assertThat(first).containsExactly(Badge.PRIMER_ENTRENAMIENTO);
        assertThat(second).isEmpty();
    }

    @Test
    void laInsigniaDeRachaYDeDiezEntrenamientosLleganConSusUmbrales() {
        BadgeEvaluator evaluator = new BadgeEvaluator();

        assertThat(evaluator.newBadges(new BadgeEvaluator.Stats(9, 4), Set.of()))
                .containsExactly(Badge.PRIMER_ENTRENAMIENTO, Badge.RACHA_4);

        assertThat(evaluator.newBadges(new BadgeEvaluator.Stats(10, 4), Set.of()))
                .containsExactly(Badge.PRIMER_ENTRENAMIENTO, Badge.RACHA_4, Badge.DIEZ_ENTRENAMIENTOS);
    }

    @Test
    void elNivelSubeCada500Xp() {
        assertThat(XpLevel.of(0).level()).isEqualTo(1);
        assertThat(XpLevel.of(499).level()).isEqualTo(1);
        assertThat(XpLevel.of(500).level()).isEqualTo(2);
        assertThat(XpLevel.of(1250).level()).isEqualTo(3);
        assertThat(XpLevel.of(1250).xpIntoLevel()).isEqualTo(250);
    }

    private static XpGrant grant(XpEventType type, int points, LocalDate date, String key) {
        return new XpGrant(type, points, date, key);
    }

    private static XpEvent event(XpEventType type, int points, LocalDate date, String key) {
        return new XpEvent(UUID.randomUUID(), USER, type, points, date, Instant.EPOCH, key);
    }
}
