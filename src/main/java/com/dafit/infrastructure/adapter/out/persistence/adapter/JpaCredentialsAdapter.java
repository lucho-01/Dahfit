package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.UserCredentials;
import com.dafit.domain.port.out.CredentialsRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaClaimCodeEntity;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaUserCredentialsEntity;
import com.dafit.infrastructure.adapter.out.persistence.support.PersistenceHash;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaCredentialsAdapter implements CredentialsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<UserCredentials> findById(UUID id) {
        return findByUserId(id);
    }

    @Override
    @Transactional
    public UserCredentials save(UserCredentials credentials) {
        return entityManager.merge(JpaUserCredentialsEntity.fromDomain(credentials)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaUserCredentialsEntity.class, id) != null;
    }

    @Override
    public Optional<UserCredentials> findByUserId(UUID userId) {
        return Optional.ofNullable(entityManager.find(JpaUserCredentialsEntity.class, userId))
                .map(JpaUserCredentialsEntity::toDomain);
    }

    @Override
    public Optional<UserCredentials> findByClaimCode(String claimCode) {
        UUID userId = entityManager.createQuery(
                        "select code.userId from JpaClaimCodeEntity code where code.codeHash = :hash",
                        UUID.class)
                .setParameter("hash", PersistenceHash.sha256(claimCode))
                .getResultStream().findFirst().orElse(null);
        return userId == null ? Optional.empty() : findByUserId(userId);
    }

    @Override
    public Optional<UserCredentials> findByEmail(String email) {
        return entityManager.createQuery(
                        "select credentials from JpaUserCredentialsEntity credentials "
                                + "where credentials.email = :email",
                        JpaUserCredentialsEntity.class)
                .setParameter("email", UserCredentials.normalizeEmail(email))
                .getResultStream().findFirst().map(JpaUserCredentialsEntity::toDomain);
    }
}
