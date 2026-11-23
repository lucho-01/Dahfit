package com.dafit.domain.port.out;

import com.dafit.domain.model.XpEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de eventos de XP. */
public interface XpEventRepository {
    Optional<XpEvent> findById(UUID id);
    XpEvent save(XpEvent xpEvent);
    boolean existsById(UUID id);
    List<XpEvent> findByUserIdAndType(UUID userId, String type);
    List<XpEvent> findRecentByUserId(UUID userId, int lookback);
    List<XpEvent> findByUserId(UUID userId);
    List<XpEvent> findBetween(UUID userId, java.time.LocalDate start, java.time.LocalDate end);
    long countCompleted(UUID userId);
    boolean existsBySourceKey(UUID userId, String sourceKey);
    long totalPoints(UUID userId);
    List<XpEvent> findRecent(UUID userId, int lookback);
}