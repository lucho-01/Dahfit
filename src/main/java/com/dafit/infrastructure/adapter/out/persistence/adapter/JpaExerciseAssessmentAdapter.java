package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.port.out.ExerciseAssessmentRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaExerciseAssessmentEntity;
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
public class JpaExerciseAssessmentAdapter implements ExerciseAssessmentRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<ExerciseAssessment> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaExerciseAssessmentEntity.class, id))
                .map(JpaExerciseAssessmentEntity::toDomain);
    }

    @Override
    @Transactional
    public ExerciseAssessment save(ExerciseAssessment assessment) {
        return entityManager.merge(JpaExerciseAssessmentEntity.fromDomain(assessment)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaExerciseAssessmentEntity.class, id) != null;
    }

    @Override
    public List<ExerciseAssessment> findByUserId(UUID userId) {
        return query("assessment.userId = :userId", "userId", userId);
    }

    @Override
    public List<ExerciseAssessment> findByExerciseId(UUID exerciseId) {
        return query("assessment.exerciseId = :exerciseId", "exerciseId", exerciseId);
    }

    @Override
    public Optional<ExerciseAssessment> findLatestFor(UUID userId, UUID exerciseId) {
        return findByUserId(userId).stream()
                .filter(assessment -> assessment.exerciseId().equals(exerciseId))
                .findFirst();
    }

    @Override
    public List<ExerciseAssessment> findLatestForUser(UUID userId) {
        LinkedHashMap<UUID, ExerciseAssessment> latest = new LinkedHashMap<>();
        findByUserId(userId).forEach(assessment -> latest.putIfAbsent(assessment.exerciseId(), assessment));
        return List.copyOf(latest.values());
    }

    private List<ExerciseAssessment> query(String condition, String parameter, UUID value) {
        return entityManager.createQuery(
                        "select assessment from JpaExerciseAssessmentEntity assessment where "
                                + condition + " order by assessment.assessedAt desc",
                        JpaExerciseAssessmentEntity.class)
                .setParameter(parameter, value)
                .getResultList().stream()
                .map(JpaExerciseAssessmentEntity::toDomain)
                .toList();
    }
}
