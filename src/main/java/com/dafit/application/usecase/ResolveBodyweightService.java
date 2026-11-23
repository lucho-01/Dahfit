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

/** Peso corporal aplicable a una evaluacion (C-05, R-11). */
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
        var closest = bodyMetrics.findClosestTo(userId, reference, lookupDays)
                .orElseThrow(this::insufficientWeight);
        if (!closest.weightKg().isPositive()) {
            throw insufficientWeight();
        }

        return closest.weightKg();
    }

    private DomainException.InsufficientData insufficientWeight() {
        return new DomainException.InsufficientData(
                "sin peso corporal registrado en +/- " + lookupDays
                        + " dias; la evaluacion queda sin fuerza relativa");
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