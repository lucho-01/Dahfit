package com.dafit.application.usecase;

import com.dafit.application.usecase.AuthorizationSupport;
import com.dafit.domain.exception.DomainException;
import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.User;
import com.dafit.domain.model.UserRole;
import com.dafit.domain.model.WeightUnit;
import com.dafit.domain.port.in.GetUserUseCase;
import com.dafit.domain.port.in.RegisterUserUseCase;
import com.dafit.domain.port.out.BodyMetricRepository;
import com.dafit.domain.port.out.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Casos de uso de perfil: consulta y alta (§3, §6).
 *
 * <ul>
 *   <li><b>R-02:</b> el rol que envia el cliente se ignora salvo que el
 *       solicitante sea ADMIN. Un alta publica siempre crea ATHLETE.</li>
 *   <li><b>R-03:</b> la consulta exige que el actor sea el propio usuario o un
 *       ADMIN.</li>
 *   <li><b>R-10:</b> el peso declarado en el alta tambien queda como primera
 *       pesada del historico ({@code users.bodyweight_kg} y
 *       {@code body_metrics} se mueven juntos).</li>
 * </ul>
 */
public class UserProfileService implements GetUserUseCase, RegisterUserUseCase {

    private final UserRepository users;
    private final BodyMetricRepository bodyMetrics;
    private final AuthorizationSupport authorization;

    public UserProfileService(UserRepository users,
                              BodyMetricRepository bodyMetrics,
                              AuthorizationSupport authorization) {
        this.users = users;
        this.bodyMetrics = bodyMetrics;
        this.authorization = authorization;
    }

    // --- GetUserUseCase ----------------------------------------------------

    @Override
    public User byId(UUID actorId, UUID userId) {
        authorization.checkSameUser(actorId, userId);
        return find(userId);
    }

    @Override
    public LocalDate today(UUID userId) {
        return LocalDate.now(find(userId).timeZone());
    }

    // --- RegisterUserUseCase ----------------------------------------------

    @Override
    public UUID register(Command command) {
        UserRole role = command.role() == null ? UserRole.ATHLETE : command.role();
        if (command.selfSignup() && role != UserRole.ATHLETE) {
            throw new DomainException.Forbidden(
                    "el alta publica solo crea atletas; coach y admin se crean desde un flujo autenticado");
        }

        Instant now = Instant.now();
        User user = new User(
                UUID.randomUUID(),
                command.displayName(),
                role,
                command.sex() == null ? Sex.UNSPECIFIED : command.sex(),
                command.birthDate(),
                command.weightKg(),
                command.preferredUnit() == null ? WeightUnit.KG : command.preferredUnit(),
                command.timeZone(),
                command.benchmarkConsent(),
                now);

        User saved = users.save(user);
        registerInitialWeight(saved, now);
        return saved.id();
    }

    private void registerInitialWeight(User user, Instant now) {
        Kg weight = user.bodyweightKg();
        if (weight == null || !weight.isPositive()) {
            return;
        }
        // Sin esto, la fuerza relativa no tendria de donde salir hasta la
        // primera pesada manual.
        bodyMetrics.save(new BodyMetric(
                UUID.randomUUID(),
                user.id(),
                weight,
                LocalDate.now(user.timeZone()),
                now,
                BodyMetric.SOURCE_MANUAL));
    }

    private User find(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new DomainException.NotFound("usuario no encontrado: " + userId));
    }
}
