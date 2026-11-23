package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.BenchmarkPoint;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Sex;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "benchmark_tables")
public class JpaBenchmarkTableEntity {

    @Id
    UUID id;

    @Column(name = "exercise_id", nullable = false)
    UUID exerciseId;

    @Column(nullable = false)
    String source;

    @Column(name = "source_version", nullable = false)
    String sourceVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Sex sex;

    @Column(name = "age_band")
    String ageBand;

    @Column(name = "bodyweight_kg")
    BigDecimal bodyweightKg;

    @Column(nullable = false)
    boolean licensed;

    @Column(name = "bodyweight_interpolation_allowed", nullable = false)
    boolean bodyweightInterpolationAllowed;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    List<JpaBenchmarkPointEntity> points = new ArrayList<>();

    protected JpaBenchmarkTableEntity() {
    }

    public static JpaBenchmarkTableEntity fromDomain(BenchmarkTable table) {
        JpaBenchmarkTableEntity entity = new JpaBenchmarkTableEntity();
        entity.id = table.id();
        entity.exerciseId = table.exerciseId();
        entity.source = table.source();
        entity.sourceVersion = table.sourceVersion();
        entity.sex = table.category().sex();
        entity.ageBand = table.category().ageBand();
        entity.bodyweightKg = valueOf(table.bodyweightKg());
        entity.licensed = table.licensed();
        entity.bodyweightInterpolationAllowed = table.bodyweightInterpolationAllowed();
        entity.createdAt = Instant.now();
        entity.points = new ArrayList<>(table.points().stream()
                .map(point -> JpaBenchmarkPointEntity.fromDomain(point, entity))
                .toList());
        return entity;
    }

    public BenchmarkTable toDomain() {
        List<BenchmarkPoint> domainPoints = points.stream()
                .map(JpaBenchmarkPointEntity::toDomain)
                .sorted(java.util.Comparator.comparing(BenchmarkPoint::percentile))
                .toList();
        return new BenchmarkTable(
                id, exerciseId, source, sourceVersion,
                new BenchmarkCategory(sex, ageBand),
                bodyweightKg == null ? null : Kg.ofKg(bodyweightKg),
                licensed, bodyweightInterpolationAllowed, domainPoints);
    }

    private static BigDecimal valueOf(Kg value) {
        return value == null ? null : value.value();
    }
}
