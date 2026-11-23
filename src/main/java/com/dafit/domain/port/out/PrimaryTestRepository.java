package com.dafit.domain.port.out;

import com.dafit.domain.model.PrimaryTest;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

/** Puerto de salida: repositorio de pruebas primarias. */
public interface PrimaryTestRepository {
    Optional<PrimaryTest> findById(UUID id);
    PrimaryTest save(PrimaryTest test);
    boolean existsById(UUID id);
    List<PrimaryTest> findByUserId(UUID userId);
    List<PrimaryTest> findAll();

    default List<PrimaryTest> saveAll(Collection<PrimaryTest> tests) {
        return tests.stream().map(this::save).toList();
    }
}