package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Credenciales de acceso de un usuario.
 *
 * <p>Viven en un agregado aparte de {@link User} a proposito: el perfil se lee
 * y se comparte a menudo, el hash de la contrasena no. El email se guarda
 * normalizado (minusculas y sin espacios) para que el login no dependa de como
 * lo escriba el usuario.
 *
 * <p><b>R-02:</b> cuando un perfil creado por un entrenador se registra con
 * email y contrasena, {@code claimedAt} queda a nulo. Solo una cuenta ya
 * reclamada puede iniciar sesion; el primer login (con el codigo de reclamacion
 * entregado al alta) la marca como reclamada. Sin esta separacion, conocer el
 * Id de un atleta sin cuenta basta para quedarse con ella.
 */
public record UserCredentials(
        UUID userId,
        String email,
        String passwordHash,
        Instant createdAt,
        Instant claimedAt
) {

    public UserCredentials {
        Objects.requireNonNull(userId, "el usuario es obligatorio");
        Objects.requireNonNull(createdAt, "la fecha de creacion es obligatoria");
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException.Validation("el hash de la contrasena es obligatorio");
        }
        email = normalizeEmail(email);
    }

    public boolean isClaimed() {
        return claimedAt != null;
    }

    /** Marca la cuenta como reclamada por su duenio. */
    public UserCredentials claim(Instant at) {
        return new UserCredentials(userId, email, passwordHash, createdAt,
                claimedAt == null ? at : claimedAt);
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            throw new DomainException.Validation("el email es obligatorio");
        }
        String normalized = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new DomainException.Validation("el email no tiene un formato valido: " + email);
        }
        return normalized;
    }

    /** Fecha local usada por los cortes diarios del usuario. */
    public LocalDate localDateIn(java.time.ZoneId zone) {
        return LocalDate.now(zone);
    }
}
