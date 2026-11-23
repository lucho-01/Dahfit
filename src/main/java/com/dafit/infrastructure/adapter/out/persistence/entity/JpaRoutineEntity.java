package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Routine;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routines")
public class JpaRoutineEntity {

    @Id
    UUID id;
    @Column(name = "owner_id", nullable = false)
    UUID ownerId;
    @Column(name = "assigned_to_id")
    UUID assignedToId;
    @Column(nullable = false)
    String name;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order ASC")
    List<JpaRoutineExerciseEntity> exercises = new ArrayList<>();

    protected JpaRoutineEntity() {
    }

    public static JpaRoutineEntity fromDomain(Routine routine) {
        JpaRoutineEntity entity = new JpaRoutineEntity();
        entity.id = routine.id();
        entity.ownerId = routine.ownerId();
        entity.assignedToId = routine.assignedToId();
        entity.name = routine.name();
        entity.createdAt = routine.createdAt();
        entity.exercises = new ArrayList<>(routine.exercises().stream()
                .map(exercise -> JpaRoutineExerciseEntity.fromDomain(exercise, entity))
                .toList());
        return entity;
    }

    public Routine toDomain() {
        return new Routine(id, ownerId, assignedToId, name,
                exercises.stream().map(JpaRoutineExerciseEntity::toDomain).toList(), createdAt);
    }
}
