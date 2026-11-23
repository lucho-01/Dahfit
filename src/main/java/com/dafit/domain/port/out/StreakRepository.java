package com.dafit.domain.port.out;

import com.dafit.domain.model.Streak;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de rachas. */
public interface StreakRepository {
    Optional<Streak> findById(UUID id);
    Streak save(Streak streak);
    boolean existsById(UUID id);
    Optional<Streak> findByUserId(UUID userId);
}