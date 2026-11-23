package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.MuscleCoefficient;
import com.dafit.domain.model.MuscleGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Entity
@Table(name = "muscle_coefficients")
public class JpaMuscleCoefficientEntity {

    @Id
    UUID id;
    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MuscleGroup muscle;
    @Column(nullable = false)
    BigDecimal coefficient;
    @Column(nullable = false)
    String version;
    @Column(nullable = false)
    boolean approved;

    protected JpaMuscleCoefficientEntity() {
    }

    public static JpaMuscleCoefficientEntity fromDomain(MuscleCoefficient value) {
        JpaMuscleCoefficientEntity entity = new JpaMuscleCoefficientEntity();
        entity.id = UUID.nameUUIDFromBytes(
                (value.exerciseId() + ":" + value.muscle() + ":" + value.version())
                        .getBytes(StandardCharsets.UTF_8));
        entity.exerciseId = value.exerciseId();
        entity.muscle = value.muscle();
        entity.coefficient = value.coefficient();
        entity.version = value.version();
        entity.approved = value.approved();
        return entity;
    }

    public MuscleCoefficient toDomain() {
        return new MuscleCoefficient(exerciseId, muscle, coefficient, version, approved);
    }
}
