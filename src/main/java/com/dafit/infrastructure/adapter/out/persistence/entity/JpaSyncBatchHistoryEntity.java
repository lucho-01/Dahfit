package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.SyncBatchHistory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sync_batches")
public class JpaSyncBatchHistoryEntity {

    @Id
    UUID id;
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(name = "received_at", nullable = false)
    Instant receivedAt;
    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    List<JpaSyncBatchResultEntity> results = new ArrayList<>();

    protected JpaSyncBatchHistoryEntity() {
    }

    public static JpaSyncBatchHistoryEntity fromDomain(SyncBatchHistory history) {
        JpaSyncBatchHistoryEntity entity = new JpaSyncBatchHistoryEntity();
        entity.id = history.id();
        entity.userId = history.userId();
        entity.receivedAt = history.receivedAt();
        entity.results = new ArrayList<>();
        for (int index = 0; index < history.results().size(); index++) {
            entity.results.add(JpaSyncBatchResultEntity.fromDomain(
                    history.results().get(index), entity, index));
        }
        return entity;
    }

    public SyncBatchHistory toDomain() {
        return new SyncBatchHistory(
                id,
                userId,
                receivedAt,
                results.stream().map(JpaSyncBatchResultEntity::toDomain).toList());
    }
}
