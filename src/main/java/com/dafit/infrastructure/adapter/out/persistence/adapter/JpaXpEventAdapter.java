package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.XpEvent;
import com.dafit.domain.model.XpEventType;
import com.dafit.domain.port.out.XpEventRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaXpEventEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaXpEventAdapter implements XpEventRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<XpEvent> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaXpEventEntity.class, id))
                .map(JpaXpEventEntity::toDomain);
    }

    @Override
    @Transactional
    public XpEvent save(XpEvent event) {
        return findBySource(event.userId(), event.sourceKey())
                .orElseGet(() -> entityManager.merge(JpaXpEventEntity.fromDomain(event)).toDomain());
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaXpEventEntity.class, id) != null;
    }

    @Override
    public List<XpEvent> findByUserIdAndType(UUID userId, String type) {
        return entityManager.createQuery(
                        "select event from JpaXpEventEntity event where event.userId = :userId "
                                + "and event.type = :type order by event.occurredAt desc",
                        JpaXpEventEntity.class)
                .setParameter("userId", userId)
                .setParameter("type", XpEventType.valueOf(type))
                .getResultList().stream().map(JpaXpEventEntity::toDomain).toList();
    }

    @Override
    public List<XpEvent> findRecentByUserId(UUID userId, int lookback) {
        return limited(userId, lookback);
    }

    @Override
    public List<XpEvent> findByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select event from JpaXpEventEntity event where event.userId = :userId "
                                + "order by event.occurredAt desc",
                        JpaXpEventEntity.class)
                .setParameter("userId", userId)
                .getResultList().stream().map(JpaXpEventEntity::toDomain).toList();
    }

    @Override
    public List<XpEvent> findBetween(UUID userId, LocalDate start, LocalDate end) {
        return entityManager.createQuery(
                        "select event from JpaXpEventEntity event where event.userId = :userId "
                                + "and event.localDate between :start and :end order by event.localDate",
                        JpaXpEventEntity.class)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList().stream().map(JpaXpEventEntity::toDomain).toList();
    }

    @Override
    public long countCompleted(UUID userId) {
        return entityManager.createQuery(
                        "select count(event) from JpaXpEventEntity event where event.userId = :userId "
                                + "and event.type = :type", Long.class)
                .setParameter("userId", userId)
                .setParameter("type", XpEventType.SESSION_COMPLETED)
                .getSingleResult();
    }

    @Override
    public boolean existsBySourceKey(UUID userId, String sourceKey) {
        return findBySource(userId, sourceKey).isPresent();
    }

    @Override
    public long totalPoints(UUID userId) {
        return entityManager.createQuery(
                        "select coalesce(sum(event.points), 0) from JpaXpEventEntity event "
                                + "where event.userId = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult();
    }

    @Override
    public List<XpEvent> findRecent(UUID userId, int lookback) {
        return limited(userId, lookback);
    }

    private List<XpEvent> limited(UUID userId, int lookback) {
        return entityManager.createQuery(
                        "select event from JpaXpEventEntity event where event.userId = :userId "
                                + "order by event.occurredAt desc", JpaXpEventEntity.class)
                .setParameter("userId", userId)
                .setMaxResults(Math.max(0, lookback))
                .getResultList().stream().map(JpaXpEventEntity::toDomain).toList();
    }

    private Optional<XpEvent> findBySource(UUID userId, String sourceKey) {
        return entityManager.createQuery(
                        "select event from JpaXpEventEntity event where event.userId = :userId "
                                + "and event.sourceKey = :sourceKey",
                        JpaXpEventEntity.class)
                .setParameter("userId", userId)
                .setParameter("sourceKey", sourceKey)
                .getResultStream().findFirst().map(JpaXpEventEntity::toDomain);
    }
}
