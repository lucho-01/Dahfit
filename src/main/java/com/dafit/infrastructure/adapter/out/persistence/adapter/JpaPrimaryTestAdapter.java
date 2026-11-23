package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.PrimaryTest;
import com.dafit.domain.port.out.PrimaryTestRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaPrimaryTestEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaPrimaryTestAdapter implements PrimaryTestRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<PrimaryTest> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaPrimaryTestEntity.class, id))
                .map(JpaPrimaryTestEntity::toDomain);
    }

    @Override
    @Transactional
    public PrimaryTest save(PrimaryTest test) {
        return entityManager.merge(JpaPrimaryTestEntity.fromDomain(test)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaPrimaryTestEntity.class, id) != null;
    }

    @Override
    public List<PrimaryTest> findByUserId(UUID userId) {
        return findAll();
    }

    @Override
    public List<PrimaryTest> findAll() {
        return entityManager.createQuery(
                        "select test from JpaPrimaryTestEntity test",
                        JpaPrimaryTestEntity.class)
                .getResultList().stream().map(JpaPrimaryTestEntity::toDomain).toList();
    }
}
