package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.Routine;
import com.dafit.domain.port.out.RoutineRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaRoutineEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaRoutineAdapter implements RoutineRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Routine> findById(UUID id) {
        return entityManager.createQuery(
                        "select distinct routine from JpaRoutineEntity routine "
                                + "left join fetch routine.exercises where routine.id = :id",
                        JpaRoutineEntity.class)
                .setParameter("id", id).getResultStream()
                .findFirst().map(JpaRoutineEntity::toDomain);
    }

    @Override
    @Transactional
    public Routine save(Routine routine) {
        return entityManager.merge(JpaRoutineEntity.fromDomain(routine)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaRoutineEntity.class, id) != null;
    }

    @Override
    public List<Routine> findByOwnerId(UUID ownerId) {
        return query("routine.ownerId = :ownerId", "ownerId", ownerId);
    }

    @Override
    public List<Routine> findAll() {
        return entityManager.createQuery(
                        "select distinct routine from JpaRoutineEntity routine "
                                + "left join fetch routine.exercises",
                        JpaRoutineEntity.class)
                .getResultList().stream().map(JpaRoutineEntity::toDomain).toList();
    }

    @Override
    public List<Routine> findOwnedBy(UUID userId) {
        return query("routine.ownerId = :userId", "userId", userId);
    }

    @Override
    public List<Routine> findAssignedTo(UUID userId) {
        return query("routine.assignedToId = :userId", "userId", userId);
    }

    private List<Routine> query(String condition, String parameter, UUID value) {
        return entityManager.createQuery(
                        "select distinct routine from JpaRoutineEntity routine "
                                + "left join fetch routine.exercises where " + condition
                                + " order by routine.createdAt desc",
                        JpaRoutineEntity.class)
                .setParameter(parameter, value)
                .getResultList().stream().map(JpaRoutineEntity::toDomain).toList();
    }
}
