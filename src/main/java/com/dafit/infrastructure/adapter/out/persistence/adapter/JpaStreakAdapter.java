package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.Streak;
import com.dafit.domain.port.out.StreakRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaStreakEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaStreakAdapter implements StreakRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Streak> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaStreakEntity.class, id))
                .map(JpaStreakEntity::toDomain);
    }

    @Override
    @Transactional
    public Streak save(Streak streak) {
        return entityManager.merge(JpaStreakEntity.fromDomain(streak)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaStreakEntity.class, id) != null;
    }

    @Override
    public Optional<Streak> findByUserId(UUID userId) {
        return findById(userId);
    }
}
