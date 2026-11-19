package com.dafit.application.usecase;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.User;
import com.dafit.domain.port.in.ResolveBodyweightUseCase;
import com.dafit.domain.port.out.BodyMetricRepository;
import com.dafit.domain.port.out.UserRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Peso corporal aplicable a una evaluacion (C-05, R-11).
 *
 * <p>La ventana se mide sobre {@code measuredOn} (el dia en que el usuario se
 * peso), no sobre {@code recordedAt} (cuando subio el dato). Con
 * {@code recordedAt}, una sincronizacion offline que subia una pesada de hace un
 * mes la volvia "reciente", y una pesada retroactiva de hoy contaba como actual.
 *
 * <p>Sin pesada en la ventana NO hay fallback a un peso lejano: se lanza
 * {@code InsufficientData} y la evaluacion queda sin fuerza relativa.
 */
public class ResolveBodyweightService implements ResolveBodyweightUseCase {

    private final BodyMetricRepository bodyMetrics;
    private final UserRepository users;
    private final int lookupDays;
    private final Clock clock;

    public ResolveBodyweightService(BodyMetricRepository bodyMetrics,
                                    UserRepository users,
                                    int lookupDays,
                                    Clock clock) {
        this.bodyMetrics = bodyMetrics;
        this.users = users;
        this.lookupDays = Math.max(1, lookupDays);
        this.clock = clock;
    }

    @Override
    public Kg resolve(UUID userId, Instant at) {
        LocalDate reference = referenceDate(userId, at);

        return bodyMetrics.findClosestTo(userId, reference, lookupDays)
                .map(BodyMetric::weightKg)
                .filter(Kg::isPositive)
                .orElseThrow(() -> new DomainException.InsufficientData(
                        "sin peso corporal registrado en +/- " + lookupDays
                                + " dias; la evaluacion queda sin fuerza relativa"));
    }

    @Override
    public LocalDate referenceDate(UUID userId, Instant at) {
        User user = findUser(userId);
        Instant effective = at == null ? clock.instant() : at;
        return ZonedDateTime.ofInstant(effective, user.timeZone()).toLocalDate();
    }

    private User findUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new DomainException.NotFound("usuario no encontrado: " + userId));
    }
}
