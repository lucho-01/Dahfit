package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.PrimaryTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Entity
@Table(name = "primary_tests")
public class JpaPrimaryTestEntity {

    @Id
    UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MuscleGroup muscle;
    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;
    @Column(name = "primary_", nullable = false)
    boolean primary;

    protected JpaPrimaryTestEntity() {
    }

    public static JpaPrimaryTestEntity fromDomain(PrimaryTest test) {
        JpaPrimaryTestEntity entity = new JpaPrimaryTestEntity();
        entity.id = UUID.nameUUIDFromBytes(
                (test.muscle() + ":" + test.exerciseId()).getBytes(StandardCharsets.UTF_8));
        entity.muscle = test.muscle();
        entity.exerciseId = test.exerciseId();
        entity.primary = test.primary();
        return entity;
    }

    public PrimaryTest toDomain() {
        return new PrimaryTest(muscle, exerciseId, primary);
    }
}
