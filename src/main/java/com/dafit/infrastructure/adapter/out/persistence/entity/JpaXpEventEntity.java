package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.XpEvent;
import com.dafit.domain.model.XpEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "xp_events")
public class JpaXpEventEntity {

    @Id
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    XpEventType type;
    @Column(nullable = false)
    int points;
    @Column(name = "local_date", nullable = false)
    LocalDate localDate;
    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;
    @Column(name = "source_key", nullable = false)
    String sourceKey;

    protected JpaXpEventEntity() {
    }

    public static JpaXpEventEntity fromDomain(XpEvent event) {
        JpaXpEventEntity entity = new JpaXpEventEntity();
        entity.id = event.id();
        entity.userId = event.userId();
        entity.type = event.type();
        entity.points = event.points();
        entity.localDate = event.localDate();
        entity.occurredAt = event.occurredAt();
        entity.sourceKey = event.sourceKey();
        return entity;
    }

    public XpEvent toDomain() {
        return new XpEvent(id, userId, type, points, localDate, occurredAt, sourceKey);
    }
}
