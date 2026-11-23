package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.BenchmarkPoint;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Percentile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "benchmark_points")
public class JpaBenchmarkPointEntity {

    @Id
    UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "benchmark_table_id", nullable = false)
    JpaBenchmarkTableEntity table;

    @Column(nullable = false, precision = 5, scale = 2)
    BigDecimal percentile;

    @Column(name = "load_kg", nullable = false)
    BigDecimal loadKg;

    protected JpaBenchmarkPointEntity() {
    }

    static JpaBenchmarkPointEntity fromDomain(BenchmarkPoint point, JpaBenchmarkTableEntity table) {
        JpaBenchmarkPointEntity entity = new JpaBenchmarkPointEntity();
        entity.id = UUID.randomUUID();
        entity.table = table;
        entity.percentile = point.percentile().value();
        entity.loadKg = point.e1rm().value();
        return entity;
    }

    BenchmarkPoint toDomain() {
        return new BenchmarkPoint(
                new Percentile(percentile),
                Kg.ofKg(loadKg));
    }
}
