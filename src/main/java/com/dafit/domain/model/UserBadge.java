package com.dafit.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Insignia conseguida por un usuario. Una sola vez por insignia. */
public record UserBadge(
        UUID id,
        UUID userId,
        Badge badge,
        Instant earnedAt
) {

    public UserBadge {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(badge, "la insignia es obligatoria");
        Objects.requireNonNull(earnedAt, "la fecha de obtencion es obligatoria");
    }
}
