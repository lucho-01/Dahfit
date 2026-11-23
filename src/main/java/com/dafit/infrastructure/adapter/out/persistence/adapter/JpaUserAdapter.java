package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.User;
import com.dafit.domain.port.out.UserRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaUserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaUserAdapter implements UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaUserEntity.class, id))
                .map(JpaUserEntity::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        return entityManager.merge(JpaUserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaUserEntity.class, id) != null;
    }

    @Override
    public Optional<User> findByDisplayName(String displayName) {
        return entityManager.createQuery(
                        "select user from JpaUserEntity user where user.displayName = :name",
                        JpaUserEntity.class)
                .setParameter("name", displayName)
                .getResultStream()
                .findFirst()
                .map(JpaUserEntity::toDomain);
    }
}
