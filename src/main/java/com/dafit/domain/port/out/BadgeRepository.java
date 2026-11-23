package com.dafit.domain.port.out;

import com.dafit.domain.model.Badge;
import com.dafit.domain.model.UserBadge;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Puerto de salida: repositorio de insignias. */
public interface BadgeRepository {
    Optional<Badge> findById(UUID id);

    /**
     * Otorga una insignia a un usuario (idempotente: una sola vez por insignia).
     *
     * <p>El {@code userId} es obligatorio: {@code user_badges} asocia la
     * insignia con quien la consigue; guardar el enum a secas no deja rastro de
     * ningun usuario.
     */
    void save(UUID userId, Badge badge);
    void save(UserBadge userBadge);

    boolean existsById(UUID id);
    List<Badge> findAll();

    /** Insignias conseguidas por un usuario, con su fecha de obtencion. */
    List<UserBadge> findByUserId(UUID userId);

    /** Conjunto de insignias ya en posesion del usuario (nunca vacio). */
    Set<Badge> badgesOf(UUID userId);
}