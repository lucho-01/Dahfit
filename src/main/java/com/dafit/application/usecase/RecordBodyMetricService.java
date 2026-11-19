package com.dafit.application.usecase;

import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.model.User;
import com.dafit.domain.port.in.RecordBodyMetricUseCase;
import com.dafit.domain.port.out.BodyMetricRepository;
import com.dafit.domain.port.out.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: registrar una pesada.
 *
 * <p>R-10: la pesada actualiza tambien el peso del perfil. Antes solo se
 * escribia en el historico y {@code GET /api/users/{id}} devolvia para siempre
 * el peso del alta, lo que ademas dejaba {@code User.canPublishBenchmarks()}
 * con un dato obsoleto.
 *
 * <p>La fecha por defecto es "hoy" en la zona del usuario, no la del servidor.
 */
public class RecordBodyMetricService implements RecordBodyMetricUseCase {

    private final BodyMetricRepository bodyMetrics;
    private final UserRepository users;
    private final AuthorizationSupport authorization;

    public RecordBodyMetricService(BodyMetricRepository bodyMetrics,
                                   UserRepository users,
                                   AuthorizationSupport authorization) {
        this.bodyMetrics = bodyMetrics;
        this.users = users;
        this.authorization = authorization;
    }

    @Override
    public UUID record(Command command) {
        authorization.checkSameUser(command.actorId(), command.userId());

        if (command.weightKg() == null || !command.weightKg().isPositive()) {
            throw new DomainException.Validation("el peso corporal debe ser positivo");
        }

        User user = users.findById(command.userId())
                .orElseThrow(() -> new DomainException.NotFound(
                        "usuario no encontrado: " + command.userId()));

        LocalDate today = LocalDate.now(user.timeZone());
        LocalDate date = command.date() == null ? today : command.date();
        if (date.isAfter(today)) {
            throw new DomainException.Validation(
                    "no se puede registrar una pesada con fecha futura: " + date);
        }

        BodyMetric metric = new BodyMetric(
                UUID.randomUUID(),
                user.id(),
                command.weightKg(),
                date,
                Instant.now(),
                BodyMetric.SOURCE_MANUAL);

        // R-10: hay que leer el ultimo dia ANTES de guardar. La version anterior
        // guardaba primero y comparaba con una lista que ya incluia la pesada
        // nueva, asi que la comparacion nunca era cierta (una pesada actual no
        // actualizaba el perfil y una retroactiva si podia sobrescribirlo).
        Optional<LocalDate> previousLatest = latestDate(user.id());
        boolean shouldUpdateProfile = user.bodyweightKg() == null
                || previousLatest.map(latest -> !date.isBefore(latest)).orElse(true);

        UUID id = bodyMetrics.save(metric).id();

        if (shouldUpdateProfile) {
            users.save(user.withBodyweight(command.weightKg()));
        }

        return id;
    }

    private Optional<LocalDate> latestDate(UUID userId) {
        return bodyMetrics.findHistory(userId).stream()
                .map(BodyMetric::date)
                .max(LocalDate::compareTo);
    }
}
