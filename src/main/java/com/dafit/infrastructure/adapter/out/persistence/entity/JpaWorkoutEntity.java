package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Workout;
import com.dafit.domain.model.WorkoutStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workouts")
public class JpaWorkoutEntity {

    @Id
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(name = "local_date", nullable = false)
    LocalDate localDate;
    @Column(name = "time_zone", nullable = false)
    String timeZone;
    @Column(name = "started_at", nullable = false)
    Instant startedAt;
    @Column(name = "finished_at")
    Instant finishedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    WorkoutStatus status;
    String notes;
    @Column(name = "bodyweight_kg")
    java.math.BigDecimal bodyweightKg;
    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    List<JpaWorkoutSetEntity> sets = new ArrayList<>();

    protected JpaWorkoutEntity() {
    }

    public static JpaWorkoutEntity fromDomain(Workout workout) {
        JpaWorkoutEntity entity = new JpaWorkoutEntity();
        entity.id = workout.id();
        entity.userId = workout.userId();
        entity.localDate = workout.localDate();
        entity.timeZone = workout.timeZone().getId();
        entity.startedAt = workout.startedAt();
        entity.finishedAt = workout.finishedAt();
        entity.status = workout.status();
        entity.notes = workout.notes();
        entity.bodyweightKg = workout.bodyweight() == null ? null : workout.bodyweight().value();
        entity.sets = new ArrayList<>(workout.sets().stream()
                .map(set -> JpaWorkoutSetEntity.fromDomain(set, entity))
                .toList());
        return entity;
    }

    public Workout toDomain() {
        return new Workout(id, userId, localDate, ZoneId.of(timeZone), startedAt, finishedAt, status,
                sets.stream().map(JpaWorkoutSetEntity::toDomain).toList(), notes,
                bodyweightKg == null ? null : Kg.ofKg(bodyweightKg));
    }
}
