package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.port.out.BodyMetricRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaBodyMetricEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaBodyMetricAdapter implements BodyMetricRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<BodyMetric> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaBodyMetricEntity.class, id))
                .map(JpaBodyMetricEntity::toDomain);
    }

    @Override
    @Transactional
    public BodyMetric save(BodyMetric metric) {
        return entityManager.merge(JpaBodyMetricEntity.fromDomain(metric)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaBodyMetricEntity.class, id) != null;
    }

    @Override
    public List<BodyMetric> findByUserId(UUID userId) {
        return queryByUser(userId).stream().map(JpaBodyMetricEntity::toDomain).toList();
    }

    @Override
    public List<BodyMetric> findRecentByUserId(UUID userId, int lookback) {
        return queryByUser(userId).stream().limit(Math.max(0, lookback))
                .map(JpaBodyMetricEntity::toDomain).toList();
    }

    @Override
    public List<BodyMetric> findHistory(UUID userId) {
        return findByUserId(userId);
    }

    @Override
    public Optional<BodyMetric> findClosestTo(UUID userId, LocalDate date, int lookback) {
        LocalDate from = date.minusDays(Math.max(0, lookback));
        LocalDate to = date.plusDays(Math.max(0, lookback));
        return queryByUser(userId).stream()
                .map(JpaBodyMetricEntity::toDomain)
                .filter(metric -> !metric.date().isBefore(from) && !metric.date().isAfter(to))
                .min(Comparator.comparingLong(metric -> Math.abs(
                        java.time.temporal.ChronoUnit.DAYS.between(metric.date(), date))));
    }

    private List<JpaBodyMetricEntity> queryByUser(UUID userId) {
        return entityManager.createQuery(
                        "select metric from JpaBodyMetricEntity metric "
                                + "where metric.userId = :userId "
                                + "order by metric.date desc, metric.recordedAt desc",
                        JpaBodyMetricEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
