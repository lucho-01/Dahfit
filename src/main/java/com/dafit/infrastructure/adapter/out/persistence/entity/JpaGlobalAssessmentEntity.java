package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.GlobalAssessment;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.StrengthRank;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "global_assessments")
public class JpaGlobalAssessmentEntity {

    @Id
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AssessmentStatus status;
    Integer points;
    @Enumerated(EnumType.STRING)
    @Column(name = "rank")
    StrengthRank rank;
    @Column(name = "muscles_evaluated", nullable = false)
    int musclesEvaluated;
    @Column(name = "muscles_required", nullable = false)
    int musclesRequired;
    @Column(name = "assessed_at", nullable = false)
    Instant assessedAt;

    protected JpaGlobalAssessmentEntity() {
    }

    public static JpaGlobalAssessmentEntity fromDomain(GlobalAssessment assessment) {
        JpaGlobalAssessmentEntity entity = new JpaGlobalAssessmentEntity();
        entity.id = assessment.id();
        entity.userId = assessment.userId();
        entity.status = assessment.status();
        entity.points = assessment.points().map(Points::value).orElse(null);
        entity.rank = assessment.rank().orElse(null);
        entity.musclesEvaluated = assessment.musclesEvaluated();
        entity.musclesRequired = assessment.musclesRequired();
        entity.assessedAt = assessment.assessedAt();
        return entity;
    }

    public GlobalAssessment toDomain() {
        return new GlobalAssessment(id, userId, status,
                Optional.ofNullable(points).map(Points::of), Optional.ofNullable(rank),
                musclesEvaluated, musclesRequired, assessedAt);
    }
}
