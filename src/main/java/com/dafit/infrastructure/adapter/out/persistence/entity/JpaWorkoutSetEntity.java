package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Kg;
import com.dafit.domain.model.LoadMode;
import com.dafit.domain.model.Reps;
import com.dafit.domain.model.Rir;
import com.dafit.domain.model.SetType;
import com.dafit.domain.model.WeightUnit;
import com.dafit.domain.model.WorkoutSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_sets")
public class JpaWorkoutSetEntity {

    @Id
    UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    JpaWorkoutEntity workout;
    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;
    @Column(name = "set_order", nullable = false)
    int order;
    @Column(name = "load_kg", nullable = false)
    BigDecimal loadKg;
    @Enumerated(EnumType.STRING)
    @Column(name = "input_unit", nullable = false)
    WeightUnit inputUnit;
    @Enumerated(EnumType.STRING)
    @Column(name = "load_mode", nullable = false)
    LoadMode loadMode;
    @Column(name = "implement_count", nullable = false)
    int implementCount;
    @Column(nullable = false)
    int reps;
    @Column(nullable = false)
    int rir;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    SetType type;
    String notes;
    @Column(name = "performed_at", nullable = false)
    Instant performedAt;
    @Column(name = "bodyweight_kg")
    BigDecimal bodyweightKg;
    @Column(name = "bodyweight_factor")
    BigDecimal bodyweightFactor;

    protected JpaWorkoutSetEntity() {
    }

    static JpaWorkoutSetEntity fromDomain(WorkoutSet set, JpaWorkoutEntity workout) {
        JpaWorkoutSetEntity entity = new JpaWorkoutSetEntity();
        entity.id = set.id();
        entity.workout = workout;
        entity.exerciseId = set.exerciseId();
        entity.order = set.order();
        entity.loadKg = set.load().value();
        entity.inputUnit = set.inputUnit();
        entity.loadMode = set.loadMode();
        entity.implementCount = set.implementCount();
        entity.reps = set.reps().value();
        entity.rir = set.rir().value();
        entity.type = set.type();
        entity.notes = set.notes();
        entity.performedAt = set.performedAt();
        entity.bodyweightKg = set.bodyweightKg() == null ? null : set.bodyweightKg().value();
        entity.bodyweightFactor = set.bodyweightFactor();
        return entity;
    }

    WorkoutSet toDomain() {
        return new WorkoutSet(id, workout.id, exerciseId, order, Kg.ofKg(loadKg), inputUnit, loadMode,
                implementCount, new Reps(reps), new Rir(rir), type, notes, performedAt,
                bodyweightKg == null ? null : Kg.ofKg(bodyweightKg), bodyweightFactor);
    }
}
