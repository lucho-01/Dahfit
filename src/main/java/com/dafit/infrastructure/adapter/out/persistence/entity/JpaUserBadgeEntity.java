package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Badge;
import com.dafit.domain.model.UserBadge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_badges")
public class JpaUserBadgeEntity {

    @Id
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Badge badge;
    @Column(name = "earned_at", nullable = false)
    Instant earnedAt;

    protected JpaUserBadgeEntity() {
    }

    public static JpaUserBadgeEntity fromDomain(UserBadge userBadge) {
        JpaUserBadgeEntity entity = new JpaUserBadgeEntity();
        entity.id = userBadge.id();
        entity.userId = userBadge.userId();
        entity.badge = userBadge.badge();
        entity.earnedAt = userBadge.earnedAt();
        return entity;
    }

    public UserBadge toDomain() {
        return new UserBadge(id, userId, badge, earnedAt);
    }
}
