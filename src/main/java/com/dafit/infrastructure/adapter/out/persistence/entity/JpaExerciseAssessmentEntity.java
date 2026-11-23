package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.model.AssessmentStatus;
import com.dafit.domain.model.E1RM;
import com.dafit.domain.model.ExerciseAssessment;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Percentile;
import com.dafit.domain.model.Points;
import com.dafit.domain.model.RelativeStrength;
import com.dafit.domain.model.StrengthRank;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "exercise_assessments")
public class JpaExerciseAssessmentEntity {

    @Id
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AssessmentStatus status;
    Integer points;
    @Enumerated(EnumType.STRING)
    @Column(name = "rank")
    StrengthRank rank;
    @Column(name = "e1rm_kg")
    BigDecimal e1rmKg;
    @Column(name = "relative_strength")
    BigDecimal relativeStrength;
    BigDecimal percentile;
    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_version", nullable = false)
    AlgorithmVersion algorithmVersion;
    @Column(name = "benchmark_table_id")
    UUID benchmarkTableId;
    @Column(name = "bodyweight_used_kg")
    BigDecimal bodyweightUsedKg;
    @Column(name = "best_set_id")
    UUID bestSetId;
    @Column(name = "assessed_at", nullable = false)
    Instant assessedAt;

    protected JpaExerciseAssessmentEntity() {
    }

    public static JpaExerciseAssessmentEntity fromDomain(ExerciseAssessment assessment) {
        JpaExerciseAssessmentEntity entity = new JpaExerciseAssessmentEntity();
        entity.id = assessment.id();
        entity.userId = assessment.userId();
        entity.exerciseId = assessment.exerciseId();
        entity.status = assessment.status();
        entity.points = assessment.points().map(Points::value).orElse(null);
        entity.rank = assessment.rank().orElse(null);
        entity.e1rmKg = assessment.e1rm().map(value -> value.value().value()).orElse(null);
        entity.relativeStrength = assessment.relativeStrength().map(RelativeStrength::value).orElse(null);
        entity.percentile = assessment.percentile().map(Percentile::value).orElse(null);
        entity.algorithmVersion = assessment.algorithmVersion();
        entity.benchmarkTableId = assessment.benchmarkTableId();
        entity.bodyweightUsedKg = assessment.bodyweightUsed().map(Kg::value).orElse(null);
        entity.bestSetId = assessment.bestSetId();
        entity.assessedAt = assessment.assessedAt();
        return entity;
    }

    public ExerciseAssessment toDomain() {
        return new ExerciseAssessment(
                id, userId, exerciseId, status,
                optional(points, Points::of), Optional.ofNullable(rank),
                optional(e1rmKg, value -> E1RM.of(Kg.ofKg(value))),
                optional(relativeStrength, RelativeStrength::new),
                optional(percentile, Percentile::of), algorithmVersion, benchmarkTableId,
                optional(bodyweightUsedKg, Kg::ofKg), bestSetId, assessedAt);
    }

    private static <T> Optional<T> optional(BigDecimal value,
                                             java.util.function.Function<BigDecimal, T> converter) {
        return Optional.ofNullable(value).map(converter);
    }

    private static Optional<Points> optional(Integer value,
                                              java.util.function.Function<Integer, Points> converter) {
        return Optional.ofNullable(value).map(converter);
    }
}
