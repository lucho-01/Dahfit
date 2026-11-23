package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.SyncBatchHistory;
import com.dafit.domain.port.out.SyncBatchRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaSyncBatchHistoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaSyncBatchAdapter implements SyncBatchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<SyncBatchHistory> findById(UUID id) {
        return entityManager.createQuery(
                        "select distinct batch from JpaSyncBatchHistoryEntity batch "
                                + "left join fetch batch.results where batch.id = :id",
                        JpaSyncBatchHistoryEntity.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .map(JpaSyncBatchHistoryEntity::toDomain);
    }

    @Override
    @Transactional
    public SyncBatchHistory save(SyncBatchHistory history) {
        return entityManager.merge(JpaSyncBatchHistoryEntity.fromDomain(history)).toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaSyncBatchHistoryEntity.class, id) != null;
    }

    @Override
    public List<SyncBatchHistory> findByUserId(UUID userId) {
        return entityManager.createQuery(
                        "select distinct batch from JpaSyncBatchHistoryEntity batch "
                                + "left join fetch batch.results where batch.userId = :userId "
                                + "order by batch.receivedAt desc, batch.id desc",
                        JpaSyncBatchHistoryEntity.class)
                .setParameter("userId", userId)
                .getResultList().stream().map(JpaSyncBatchHistoryEntity::toDomain).toList();
    }
}
