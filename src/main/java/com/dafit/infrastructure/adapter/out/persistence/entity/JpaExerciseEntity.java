package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Equipment;
import com.dafit.domain.model.Exercise;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.MuscleGroup;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class JpaExerciseEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Equipment equipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "load_mode", nullable = false)
    LoadMode loadMode;

    @Column(nullable = false)
    boolean unilateral;

    @ElementCollection
    @CollectionTable(name = "exercise_muscles", joinColumns = @JoinColumn(name = "exercise_id"))
    @Column(name = "muscle", nullable = false)
    @Enumerated(EnumType.STRING)
    Set<MuscleGroup> muscles = new HashSet<>();

    @Column(nullable = false)
    boolean evaluable;

    @Column(name = "bodyweight_factor")
    BigDecimal bodyweightFactor;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected JpaExerciseEntity() {
    }

    public static JpaExerciseEntity fromDomain(Exercise exercise) {
        JpaExerciseEntity entity = new JpaExerciseEntity();
        entity.id = exercise.id();
        entity.name = exercise.name();
        entity.equipment = exercise.equipment();
        entity.loadMode = exercise.loadMode();
        entity.unilateral = exercise.unilateral();
        entity.muscles = new HashSet<>(exercise.muscles());
        entity.evaluable = exercise.evaluable();
        entity.bodyweightFactor = exercise.bodyweightFactor();
        entity.active = exercise.active();
        entity.createdAt = Instant.now();
        return entity;
    }

    public Exercise toDomain() {
        return new Exercise(id, name, equipment, loadMode, unilateral, muscles,
                evaluable, bodyweightFactor, active);
    }
}
