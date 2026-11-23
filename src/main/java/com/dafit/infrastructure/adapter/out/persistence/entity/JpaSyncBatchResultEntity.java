package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.SyncOutcome;
import com.dafit.domain.model.SyncResult;
import com.dafit.domain.model.SyncTarget;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "sync_batch_results")
public class JpaSyncBatchResultEntity {

    @Id
    UUID id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    JpaSyncBatchHistoryEntity batch;
    @Column(name = "item_order", nullable = false)
    int itemOrder;
    @Column(name = "item_key", nullable = false, length = 128)
    String key;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    SyncTarget target;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    SyncOutcome outcome;
    @Column(columnDefinition = "text")
    String message;

    protected JpaSyncBatchResultEntity() {
    }

    static JpaSyncBatchResultEntity fromDomain(
            SyncResult result, JpaSyncBatchHistoryEntity batch, int itemOrder) {
        JpaSyncBatchResultEntity entity = new JpaSyncBatchResultEntity();
        entity.id = UUID.randomUUID();
        entity.batch = batch;
        entity.itemOrder = itemOrder;
        entity.key = result.key();
        entity.target = result.target();
        entity.outcome = result.outcome();
        entity.message = result.message();
        return entity;
    }

    SyncResult toDomain() {
        return new SyncResult(key, target, outcome, message);
    }
}
