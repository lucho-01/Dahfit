package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutStatus;
import com.dafit.domain.port.out.WorkoutRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaWorkoutEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaWorkoutAdapter implements WorkoutRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Workout> findById(UUID id) {
        return query("workout.id = :id", "id", id).stream().findFirst();
    }

    @Override
    @Transactional
    public Workout save(Workout workout) {
        return entityManager.merge(JpaWorkoutEntity.fromDomain(workout)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaWorkoutEntity.class, id) != null;
    }

    @Override
    public List<Workout> findByUserId(UUID userId) {
        return query("workout.userId = :userId", "userId", userId);
    }

    @Override
    public List<Workout> findRecent(UUID userId, int lookback) {
        return query("workout.userId = :userId", "userId", userId)
                .stream().limit(Math.max(0, lookback)).toList();
    }

    @Override
    public List<Workout> findCompletedByUserId(UUID userId) {
        return query("workout.userId = :userId and workout.status = :status",
                "userId", userId, "status", WorkoutStatus.COMPLETED);
    }

    @Override
    public long countCompleted(UUID userId) {
        return entityManager.createQuery(
                        "select count(workout) from JpaWorkoutEntity workout "
                                + "where workout.userId = :userId and workout.status = :status",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("status", WorkoutStatus.COMPLETED)
                .getSingleResult();
    }

    @Override
    public List<Workout> findCompletedBetween(UUID userId, LocalDate start, LocalDate end) {
        return entityManager.createQuery(
                        "select distinct workout from JpaWorkoutEntity workout "
                                + "left join fetch workout.sets where workout.userId = :userId "
                                + "and workout.status = :status and workout.localDate between :start and :end "
                                + "order by workout.localDate desc",
                        JpaWorkoutEntity.class)
                .setParameter("userId", userId)
                .setParameter("status", WorkoutStatus.COMPLETED)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList().stream().map(JpaWorkoutEntity::toDomain).toList();
    }

    @Override
    public Optional<Workout> findInProgress(UUID userId) {
        return query("workout.userId = :userId and workout.status = :status",
                "userId", userId, "status", WorkoutStatus.IN_PROGRESS).stream().findFirst();
    }

    private List<Workout> query(String condition, String parameter, Object value) {
        return query(condition, parameter, value, null, null);
    }

    private List<Workout> query(String condition, String parameter, Object value,
                                String secondParameter, Object secondValue) {
        var query = entityManager.createQuery(
                "select distinct workout from JpaWorkoutEntity workout "
                        + "left join fetch workout.sets where " + condition
                        + " order by workout.localDate desc, workout.startedAt desc",
                JpaWorkoutEntity.class);
        query.setParameter(parameter, value);
        if (secondParameter != null) {
            query.setParameter(secondParameter, secondValue);
        }
        return query.getResultList().stream().map(JpaWorkoutEntity::toDomain).toList();
    }
}
