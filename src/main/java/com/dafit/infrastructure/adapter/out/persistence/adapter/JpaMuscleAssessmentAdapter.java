package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.port.out.MuscleAssessmentRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaMuscleAssessmentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaMuscleAssessmentAdapter implements MuscleAssessmentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<MuscleAssessment> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaMuscleAssessmentEntity.class, id))
                .map(JpaMuscleAssessmentEntity::toDomain);
    }

    @Override
    @Transactional
    public MuscleAssessment save(MuscleAssessment assessment) {
        return entityManager.merge(JpaMuscleAssessmentEntity.fromDomain(assessment)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaMuscleAssessmentEntity.class, id) != null;
    }

    @Override
    public List<MuscleAssessment> findByUserId(UUID userId) {
        return query("assessment.userId = :userId", "userId", userId);
    }

    @Override
    public List<MuscleAssessment> findByMuscleGroup(UUID userId, MuscleGroup muscle) {
        return entityManager.createQuery(
                        "select assessment from JpaMuscleAssessmentEntity assessment "
                                + "where assessment.userId = :userId and assessment.muscle = :muscle "
                                + "order by assessment.assessedAt desc",
                        JpaMuscleAssessmentEntity.class)
                .setParameter("userId", userId)
                .setParameter("muscle", muscle)
                .getResultList().stream().map(JpaMuscleAssessmentEntity::toDomain).toList();
    }

    @Override
    public Optional<MuscleAssessment> findLatestFor(UUID userId, MuscleGroup muscle) {
        return findByMuscleGroup(userId, muscle).stream().findFirst();
    }

    @Override
    public List<MuscleAssessment> findLatestForUser(UUID userId) {
        LinkedHashMap<MuscleGroup, MuscleAssessment> latest = new LinkedHashMap<>();
        findByUserId(userId).forEach(assessment -> latest.putIfAbsent(assessment.muscle(), assessment));
        return List.copyOf(latest.values());
    }

    private List<MuscleAssessment> query(String condition, String parameter, UUID value) {
        return entityManager.createQuery(
                        "select assessment from JpaMuscleAssessmentEntity assessment where "
                                + condition + " order by assessment.assessedAt desc",
                        JpaMuscleAssessmentEntity.class)
                .setParameter(parameter, value)
                .getResultList().stream().map(JpaMuscleAssessmentEntity::toDomain).toList();
    }
}
