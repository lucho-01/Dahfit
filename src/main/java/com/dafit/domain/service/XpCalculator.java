package com.dafit.domain.service;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.XpEvent;
import com.dafit.domain.model.XpEventType;
import com.dafit.domain.model.XpGrant;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Aplica los topes de XP de C-19.
 *
 * <p>Reglas, en este orden:
 * <ol>
 *   <li>Idempotencia: una {@code sourceKey} ya otorgada (o repetida en el lote)
 *       se descarta.</li>
 *   <li>Maximo de sesiones que dan XP por dia (por defecto 2).</li>
 *   <li>Tope diario y tope semanal (semana ISO, lunes a domingo).</li>
 * </ol>
 *
 * <p>Si el tope deja menos XP del propuesto, se otorga la parte que cabe; si no
 * cabe nada, el evento no se crea. Nunca se otorga XP negativo.
 */
public class XpCalculator {

    private final int dailyCap;
    private final int weeklyCap;
    private final int maxSessionsPerDay;

    public XpCalculator(int dailyCap, int weeklyCap, int maxSessionsPerDay) {
        if (dailyCap < 0 || weeklyCap < 0 || maxSessionsPerDay < 0) {
            throw new DomainException.Validation("los topes de XP no pueden ser negativos");
        }
        if (weeklyCap < dailyCap) {
            throw new DomainException.Validation(
                    "el tope semanal no puede ser menor que el diario");
        }
        this.dailyCap = dailyCap;
        this.weeklyCap = weeklyCap;
        this.maxSessionsPerDay = maxSessionsPerDay;
    }

    public List<XpGrant> apply(UUID userId,
                               List<XpGrant> proposals,
                               List<XpEvent> alreadyAwarded) {

        Set<String> usedKeys = new HashSet<>();
        alreadyAwarded.forEach(event -> usedKeys.add(event.sourceKey()));

        // "Libro" de eventos: los ya guardados mas los aceptados en este lote.
        // Asi dos propuestas del mismo dia compiten por el mismo tope.
        List<XpEvent> booked = new ArrayList<>(alreadyAwarded);
        List<XpGrant> accepted = new ArrayList<>();

        for (XpGrant proposal : proposals) {
            if (!usedKeys.add(proposal.sourceKey())) {
                continue;
            }

            LocalDate day = proposal.localDate();
            LocalDate weekStart = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);

            int dayPoints = booked.stream()
                    .filter(event -> event.localDate().equals(day))
                    .mapToInt(XpEvent::points)
                    .sum();
            int weekPoints = booked.stream()
                    .filter(event -> !event.localDate().isBefore(weekStart)
                            && !event.localDate().isAfter(weekEnd))
                    .mapToInt(XpEvent::points)
                    .sum();

            if (proposal.type() == XpEventType.SESSION_COMPLETED
                    && sessionsOn(booked, day) >= maxSessionsPerDay) {
                continue;
            }

            int allowed = Math.min(proposal.points(),
                    Math.min(dailyCap - dayPoints, weeklyCap - weekPoints));
            if (allowed <= 0) {
                continue;
            }

            XpGrant grant = new XpGrant(proposal.type(), allowed, day, proposal.sourceKey());
            accepted.add(grant);
            booked.add(new XpEvent(
                    UUID.randomUUID(), userId, grant.type(), grant.points(),
                    grant.localDate(), java.time.Instant.EPOCH, grant.sourceKey()));
        }

        return accepted;
    }

    private static long sessionsOn(List<XpEvent> events, LocalDate day) {
        return events.stream()
                .filter(event -> event.localDate().equals(day))
                .filter(event -> event.type() == XpEventType.SESSION_COMPLETED)
                .count();
    }
}
