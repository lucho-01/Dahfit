package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.ClaimCode;
import com.dafit.domain.port.out.ClaimCodeRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaClaimCodeEntity;
import com.dafit.infrastructure.adapter.out.persistence.support.PersistenceHash;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaClaimCodeAdapter implements ClaimCodeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public java.util.Optional<ClaimCode> findById(UUID id) {
        return java.util.Optional.ofNullable(entityManager.find(JpaClaimCodeEntity.class, id))
                .map(JpaClaimCodeEntity::toDomain);
    }

    @Override
    @Transactional
    public ClaimCode save(UUID userId, ClaimCode claimCode) {
        return entityManager.merge(JpaClaimCodeEntity.fromDomain(userId, claimCode)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaClaimCodeEntity.class, id) != null;
    }

    @Override
    public ClaimCode findByCode(String code) {
        return entityManager.createQuery(
                        "select claimCode from JpaClaimCodeEntity claimCode where claimCode.codeHash = :hash",
                        JpaClaimCodeEntity.class)
                .setParameter("hash", PersistenceHash.sha256(code))
                .getResultStream().findFirst().map(JpaClaimCodeEntity::toDomain).orElse(null);
    }

    @Override
    public ClaimCode findByUserId(UUID userId) {
        return findById(userId).orElse(null);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        JpaClaimCodeEntity code = entityManager.find(JpaClaimCodeEntity.class, userId);
        if (code != null) {
            entityManager.remove(code);
        }
    }
}
