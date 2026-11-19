package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Codigo de reclamacion de una cuenta creada en nombre de otra persona (R-02).
 *
 * <p>Se genera al registrar credenciales de un perfil sin sesion y se guarda
 * hasheado. El unico sitio donde se ve en claro es la respuesta de ese alta, que
 * viaja por un canal ya autenticado. Caduca.
 */
public record ClaimCode(
        String codeHash,
        Instant expiresAt
) {

    public ClaimCode {
        if (codeHash == null || codeHash.isBlank()) {
            throw new DomainException.Validation("el hash del codigo es obligatorio");
        }
        Objects.requireNonNull(expiresAt, "la caducidad del codigo es obligatoria");
    }

    public boolean isValidAt(Instant now) {
        return now.isBefore(expiresAt);
    }

    public boolean canBeReplacedAt(Instant now) {
        // Se permite reemitir un codigo caducado sin exigir el anterior.
        return !isValidAt(now);
    }

    public static Instant defaultExpiry(Instant from, int minutes) {
        return from.plus(Duration.ofMinutes(minutes));
    }
}
