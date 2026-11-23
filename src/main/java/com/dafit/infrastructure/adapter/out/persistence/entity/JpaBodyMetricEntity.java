package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.BodyMetric;
import com.dafit.domain.model.Kg;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "body_metrics")
public class JpaBodyMetricEntity {

    @Id
    UUID id;

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "weight_kg", nullable = false)
    java.math.BigDecimal weightKg;

    @Column(name = "measured_on", nullable = false)
    LocalDate date;

    @Column(name = "recorded_at", nullable = false)
    Instant recordedAt;

    @Column(nullable = false)
    String source;

    protected JpaBodyMetricEntity() {
    }

    public static JpaBodyMetricEntity fromDomain(BodyMetric metric) {
        JpaBodyMetricEntity entity = new JpaBodyMetricEntity();
        entity.id = metric.id();
        entity.userId = metric.userId();
        entity.weightKg = metric.weightKg().value();
        entity.date = metric.date();
        entity.recordedAt = metric.recordedAt();
        entity.source = metric.source();
        return entity;
    }

    public BodyMetric toDomain() {
        return new BodyMetric(id, userId, Kg.ofKg(weightKg), date, recordedAt, source);
    }
}
