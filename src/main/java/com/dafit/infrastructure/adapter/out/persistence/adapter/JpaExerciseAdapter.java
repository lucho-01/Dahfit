package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.Exercise;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.port.out.ExerciseRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaExerciseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaExerciseAdapter implements ExerciseRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Exercise> findById(UUID id) {
        return entityManager.createQuery(
                        "select distinct exercise from JpaExerciseEntity exercise "
                                + "left join fetch exercise.muscles where exercise.id = :id",
                        JpaExerciseEntity.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .map(JpaExerciseEntity::toDomain);
    }

    @Override
    @Transactional
    public Exercise save(Exercise exercise) {
        return entityManager.merge(JpaExerciseEntity.fromDomain(exercise)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaExerciseEntity.class, id) != null;
    }

    @Override
    public List<Exercise> findAll() {
        return list("select distinct exercise from JpaExerciseEntity exercise "
                + "left join fetch exercise.muscles");
    }

    @Override
    public List<Exercise> findAllActive() {
        return list("select distinct exercise from JpaExerciseEntity exercise "
                + "left join fetch exercise.muscles where exercise.active = true");
    }

    @Override
    public List<Exercise> search(Set<MuscleGroup> muscles, Equipment equipment, boolean evaluableOnly) {
        return findAll().stream()
                .filter(Exercise::active)
                .filter(exercise -> !evaluableOnly || exercise.evaluable())
                .filter(exercise -> equipment == null || exercise.equipment() == equipment)
                .filter(exercise -> muscles == null || muscles.isEmpty()
                        || exercise.muscles().stream().anyMatch(muscles::contains))
                .toList();
    }

    private List<Exercise> list(String jpql) {
        return entityManager.createQuery(jpql, JpaExerciseEntity.class)
                .getResultList().stream().map(JpaExerciseEntity::toDomain).toList();
    }
}
