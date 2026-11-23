package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.Badge;
import com.dafit.domain.model.UserBadge;
import com.dafit.domain.port.out.BadgeRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaUserBadgeEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class JpaBadgeAdapter implements BadgeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Badge> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaUserBadgeEntity.class, id))
                .map(JpaUserBadgeEntity::toDomain)
                .map(UserBadge::badge);
    }

    @Override
    @Transactional
    public void save(UUID userId, Badge badge) {
        save(new UserBadge(UUID.randomUUID(), userId, badge, Instant.now()));
    }

    @Override
    @Transactional
    public void save(UserBadge userBadge) {
        boolean alreadyEarned = entityManager.createQuery(
                        "select count(badge) from JpaUserBadgeEntity badge "
                                + "where badge.userId = :userId and badge.badge = :badge",
                        Long.class)
                .setParameter("userId", userBadge.userId())
                .setParameter("badge", userBadge.badge())
                .getSingleResult() > 0;
        if (alreadyEarned) {
            return;
        }
        entityManager.merge(JpaUserBadgeEntity.fromDomain(userBadge));
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaUserBadgeEntity.class, id) != null;
    }

    @Override
    public List<Badge> findAll() {
        return entityManager.createQuery(
                        "select distinct badge.badge from JpaUserBadgeEntity badge",
                        Badge.class)
                .getResultList();
    }

    @Override
    public List<UserBadge> findByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select badge from JpaUserBadgeEntity badge where badge.userId = :userId",
                        JpaUserBadgeEntity.class)
                .setParameter("userId", userId)
                .getResultList().stream().map(JpaUserBadgeEntity::toDomain).toList();
    }

    @Override
    public Set<Badge> badgesOf(UUID userId) {
        return findByUserId(userId).stream().map(UserBadge::badge).collect(Collectors.toSet());
    }
}
