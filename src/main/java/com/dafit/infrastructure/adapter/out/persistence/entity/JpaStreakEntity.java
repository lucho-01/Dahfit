package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Streak;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "streaks")
public class JpaStreakEntity {

    @Id
    @Column(name = "user_id")
    UUID userId;
    @Column(name = "current_days", nullable = false)
    int currentDays;
    @Column(name = "longest_days", nullable = false)
    int longestDays;
    @Column(name = "last_activity_date")
    LocalDate lastActivityDate;

    protected JpaStreakEntity() {
    }

    public static JpaStreakEntity fromDomain(Streak streak) {
        JpaStreakEntity entity = new JpaStreakEntity();
        entity.userId = streak.userId();
        entity.currentDays = streak.currentDays();
        entity.longestDays = streak.longestDays();
        entity.lastActivityDate = streak.lastActivityDate();
        return entity;
    }

    public Streak toDomain() {
        return new Streak(userId, currentDays, longestDays, lastActivityDate);
    }
}
