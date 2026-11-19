package com.dafit.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Resumen de gamificacion del usuario (C-19).
 *
 * <p>El XP es independiente del rango de fuerza: esta respuesta no incluye
 * ninguna evaluacion ni influye en ella.
 */
public record GamificationResponse(
        int totalXp,
        int level,
        int xpIntoLevel,
        int xpForNextLevel,
        int currentStreakDays,
        int longestStreakDays,
        LocalDate lastActivityDate,
        List<BadgeResponse> badges,
        List<XpEventResponse> recentEvents
) {

    public record BadgeResponse(
            String badge,
            String label,
            Instant earnedAt
    ) {
    }

    public record XpEventResponse(
            UUID id,
            String type,
            int points,
            LocalDate date,
            Instant occurredAt
    ) {
    }
}
