package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.MuscleAssessment;
import com.dafit.domain.model.MuscleGroup;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.StrengthRank;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "muscle_assessments")
public class JpaMuscleAssessmentEntity {

    @Id
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MuscleGroup muscle;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AssessmentStatus status;
    Integer points;
    @Enumerated(EnumType.STRING)
    @Column(name = "rank")
    StrengthRank rank;
    BigDecimal coverage;
    @ElementCollection
    @CollectionTable(name = "muscle_assessment_exercises",
            joinColumns = @JoinColumn(name = "muscle_assessment_id"))
    @Column(name = "exercise_id")
    List<UUID> exercisesEvaluated = new ArrayList<>();
    @Column(name = "primary_tests_evaluated", nullable = false)
    int primaryTestsEvaluated;
    @Column(name = "assessed_at", nullable = false)
    Instant assessedAt;

    protected JpaMuscleAssessmentEntity() {
    }

    public static JpaMuscleAssessmentEntity fromDomain(MuscleAssessment assessment) {
        JpaMuscleAssessmentEntity entity = new JpaMuscleAssessmentEntity();
        entity.id = assessment.id();
        entity.userId = assessment.userId();
        entity.muscle = assessment.muscle();
        entity.status = assessment.status();
        entity.points = assessment.points().map(Points::value).orElse(null);
        entity.rank = assessment.rank().orElse(null);
        entity.coverage = assessment.coverage();
        entity.exercisesEvaluated = new ArrayList<>(assessment.exercisesEvaluated());
        entity.primaryTestsEvaluated = assessment.primaryTestsEvaluated();
        entity.assessedAt = assessment.assessedAt();
        return entity;
    }

    public MuscleAssessment toDomain() {
        return new MuscleAssessment(id, userId, muscle, status,
                Optional.ofNullable(points).map(Points::of), Optional.ofNullable(rank),
                coverage, exercisesEvaluated, primaryTestsEvaluated, assessedAt);
    }
}
