package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.port.out.GlobalAssessmentRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaGlobalAssessmentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaGlobalAssessmentAdapter implements GlobalAssessmentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<GlobalAssessment> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaGlobalAssessmentEntity.class, id))
                .map(JpaGlobalAssessmentEntity::toDomain);
    }

    @Override
    @Transactional
    public GlobalAssessment save(GlobalAssessment assessment) {
        return entityManager.merge(JpaGlobalAssessmentEntity.fromDomain(assessment)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaGlobalAssessmentEntity.class, id) != null;
    }

    @Override
    public List<GlobalAssessment> findByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select assessment from JpaGlobalAssessmentEntity assessment "
                                + "where assessment.userId = :userId order by assessment.assessedAt desc",
                        JpaGlobalAssessmentEntity.class)
                .setParameter("userId", userId)
                .getResultList().stream().map(JpaGlobalAssessmentEntity::toDomain).toList();
    }

    @Override
    public Optional<GlobalAssessment> findLatestFor(UUID userId) {
        return findByUserId(userId).stream().findFirst();
    }
}
