package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.MuscleCoefficient;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.port.out.MuscleCoefficientRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaMuscleCoefficientEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaMuscleCoefficientAdapter implements MuscleCoefficientRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<MuscleCoefficient> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaMuscleCoefficientEntity.class, id))
                .map(JpaMuscleCoefficientEntity::toDomain);
    }

    @Override
    @Transactional
    public MuscleCoefficient save(MuscleCoefficient coefficient) {
        return entityManager.merge(JpaMuscleCoefficientEntity.fromDomain(coefficient)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaMuscleCoefficientEntity.class, id) != null;
    }

    @Override
    public List<MuscleCoefficient> findByVersion(String version) {
        return query("coefficient.version = :version", "version", version);
    }

    @Override
    public List<MuscleCoefficient> findByMuscleGroup(MuscleGroup muscle) {
        return query("coefficient.muscle = :muscle", "muscle", muscle);
    }

    @Override
    public List<MuscleCoefficient> findApproved() {
        return query("coefficient.approved = true", null, null);
    }

    @Override
    public List<MuscleCoefficient> findEffective() {
        return findApproved().stream().filter(MuscleCoefficient::isEffective).toList();
    }

    private List<MuscleCoefficient> query(String condition, String parameter, Object value) {
        var query = entityManager.createQuery(
                "select coefficient from JpaMuscleCoefficientEntity coefficient where " + condition,
                JpaMuscleCoefficientEntity.class);
        if (parameter != null) {
            query.setParameter(parameter, value);
        }
        return query.getResultList().stream().map(JpaMuscleCoefficientEntity::toDomain).toList();
    }
}
