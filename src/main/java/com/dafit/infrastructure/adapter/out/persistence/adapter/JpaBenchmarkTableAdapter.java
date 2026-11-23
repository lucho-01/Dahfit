package com.dafit.infrastructure.adapter.out.persistence.adapter;

import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Sex;
import com.dafit.domain.port.out.BenchmarkTableRepository;
import com.dafit.infrastructure.adapter.out.persistence.entity.JpaBenchmarkTableEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class JpaBenchmarkTableAdapter implements BenchmarkTableRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<BenchmarkTable> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(JpaBenchmarkTableEntity.class, id))
                .map(JpaBenchmarkTableEntity::toDomain);
    }

    @Override
    @Transactional
    public BenchmarkTable save(BenchmarkTable table) {
        JpaBenchmarkTableEntity entity = JpaBenchmarkTableEntity.fromDomain(table);
        JpaBenchmarkTableEntity saved = entityManager.merge(entity);
        return saved.toDomain();
    }

    @Override
    public boolean existsById(UUID id) {
        return entityManager.find(JpaBenchmarkTableEntity.class, id) != null;
    }

    @Override
    public List<BenchmarkTable> findAll() {
        return entityManager.createQuery(
                        "select distinct table from JpaBenchmarkTableEntity table "
                                + "left join fetch table.points",
                        JpaBenchmarkTableEntity.class)
                .getResultList().stream()
                .map(JpaBenchmarkTableEntity::toDomain)
                .toList();
    }

    @Override
    public List<BenchmarkTable> findByCategory(Sex sex) {
        return entityManager.createQuery(
                        "select distinct table from JpaBenchmarkTableEntity table "
                                + "left join fetch table.points where table.sex = :sex",
                        JpaBenchmarkTableEntity.class)
                .setParameter("sex", sex)
                .getResultList().stream()
                .map(JpaBenchmarkTableEntity::toDomain)
                .toList();
    }

    @Override
    public List<BenchmarkTable> findSiblings(UUID exerciseId, BenchmarkCategory category) {
        return candidates(exerciseId, category).stream()
                .sorted(Comparator.comparing(BenchmarkTable::bodyweightKg,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public Optional<BenchmarkTable> findFor(UUID exerciseId, BenchmarkCategory category, Kg bodyweight) {
        List<BenchmarkTable> candidates = candidates(exerciseId, category);
        return candidates.stream()
                .filter(table -> matchesWeightBand(table, bodyweight))
                .min(BenchmarkTable.selectionOrder());
    }

    private List<BenchmarkTable> candidates(UUID exerciseId, BenchmarkCategory category) {
        return entityManager.createQuery(
                        "select distinct table from JpaBenchmarkTableEntity table "
                                + "left join fetch table.points "
                                + "where table.exerciseId = :exerciseId and table.sex = :sex",
                        JpaBenchmarkTableEntity.class)
                .setParameter("exerciseId", exerciseId)
                .setParameter("sex", category.sex())
                .getResultList().stream()
                .map(JpaBenchmarkTableEntity::toDomain)
                .filter(table -> category.ageBand() == null
                        ? table.category().ageBand() == null
                        : category.ageBand().equals(table.category().ageBand()))
                .toList();
    }

    private boolean matchesWeightBand(BenchmarkTable table, Kg bodyweight) {
        if (table.bodyweightKg() == null) {
            return true;
        }
        return bodyweight != null
                && table.bodyweightKg().value().compareTo(bodyweight.value()) == 0;
    }
}
