package com.dafit.domain.port.out;

import com.dafit.domain.model.BodyMetric;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de métricas corporales. */
public interface BodyMetricRepository {
    Optional<BodyMetric> findById(UUID id);
    BodyMetric save(BodyMetric metric);
    boolean existsById(UUID id);
    List<BodyMetric> findByUserId(UUID userId);
    List<BodyMetric> findRecentByUserId(UUID userId, int lookback);
    List<BodyMetric> findHistory(UUID userId);
    Optional<BodyMetric> findClosestTo(UUID userId, LocalDate date, int lookback);
}