package com.dafit.domain.port.out;

import com.dafit.domain.model.BenchmarkTable;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.BenchmarkCategory;
import com.dafit.domain.model.Kg;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de tablas de referencias. */
public interface BenchmarkTableRepository {
    Optional<BenchmarkTable> findById(UUID id);
    BenchmarkTable save(BenchmarkTable table);
    boolean existsById(UUID id);
    List<BenchmarkTable> findAll();
    List<BenchmarkTable> findByCategory(Sex sex);
    List<BenchmarkTable> findSiblings(UUID exerciseId, BenchmarkCategory category);
    Optional<BenchmarkTable> findFor(UUID uuid, BenchmarkCategory category, Kg kg);
}