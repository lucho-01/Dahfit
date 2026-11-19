package com.dafit.domain.model;

import com.dafit.domain.model.Kg;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuario de DAFIT (§6).
 *
 * <p>El peso corporal actual es una comodidad de lectura; el valor usado en los
 * calculos lo resuelve {@code ResolveBodyweightUseCase} con la regla C-05.
 */
public record User(
        UUID id,
        String displayName,
        UserRole role,
        Sex sex,
        LocalDate birthDate,
        Kg bodyweightKg,
        WeightUnit preferredUnit,
        ZoneId timeZone,
        boolean benchmarkConsent,
        Instant createdAt
) {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Bogota");

    public User {
        Objects.requireNonNull(id, "el id es obligatorio");
        Objects.requireNonNull(createdAt, "la fecha de alta es obligatoria");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("el nombre visible es obligatorio");
        }
        role = role == null ? UserRole.ATHLETE : role;
        preferredUnit = preferredUnit == null ? WeightUnit.KG : preferredUnit;
        timeZone = timeZone == null ? DEFAULT_ZONE : timeZone;
        sex = sex == null ? Sex.UNSPECIFIED : sex;
    }

    /**
     * §7.4 - solo se puede consultar una tabla de percentiles si hay categoria
     * de referencia, peso corporal y consentimiento explicito.
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isCoach() {
        return role == UserRole.COACH;
    }

    public boolean canPublishBenchmarks() {
        return benchmarkConsent
                && sex != Sex.UNSPECIFIED
                && bodyweightKg != null
                && bodyweightKg.isPositive();
    }

    public User withBodyweight(Kg newWeight) {
        return new User(id, displayName, role, sex, birthDate, newWeight,
                preferredUnit, timeZone, benchmarkConsent, createdAt);
    }

    /**
     * Zona horaria aplicable a una operacion del usuario.
     *
     * <p>{@code hint} (la zona que envia el cliente en ese momento) gana solo
     * si es valida; si viene nula, en blanco o mal formada se usa la del
     * perfil. Asi un campo opcional tortuoso nunca tumba una peticion con un
     * 500 por una {@code DateTimeException}.
     */
    public static ZoneId resolveTimeZone(ZoneId profileZone, String hint) {
        if (hint == null || hint.isBlank()) {
            return profileZone;
        }
        try {
            return ZoneId.of(hint.trim());
        } catch (RuntimeException invalid) {
            return profileZone;
        }
    }
}
