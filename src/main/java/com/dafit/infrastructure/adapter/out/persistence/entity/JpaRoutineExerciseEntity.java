package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Reps;
import com.dafit.domain.model.RoutineExercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "routine_exercises")
public class JpaRoutineExerciseEntity {

    @Id
    UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    JpaRoutineEntity routine;
    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;
    @Column(name = "exercise_order", nullable = false)
    int order;
    @Column(name = "target_sets", nullable = false)
    int targetSets;
    @Column(name = "target_reps", nullable = false)
    int targetReps;
    @Column(name = "rest_seconds")
    Integer restSeconds;

    protected JpaRoutineExerciseEntity() {
    }

    static JpaRoutineExerciseEntity fromDomain(RoutineExercise exercise, JpaRoutineEntity routine) {
        JpaRoutineExerciseEntity entity = new JpaRoutineExerciseEntity();
        entity.id = UUID.randomUUID();
        entity.routine = routine;
        entity.exerciseId = exercise.exerciseId();
        entity.order = exercise.order();
        entity.targetSets = exercise.targetSets();
        entity.targetReps = exercise.targetReps().value();
        entity.restSeconds = exercise.restSeconds();
        return entity;
    }

    RoutineExercise toDomain() {
        return new RoutineExercise(exerciseId, order, targetSets, new Reps(targetReps), restSeconds);
    }
}
