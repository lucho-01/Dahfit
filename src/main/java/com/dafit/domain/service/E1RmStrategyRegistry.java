package com.dafit.domain.service;

import com.dafit.domain.model.AlgorithmVersion;
import com.dafit.domain.exception.DomainException;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registro de estrategias de calculo por version.
 *
 * <p>Permite que el caso de uso pida una version concreta sin conocer las
 * implementaciones. Anadir un algoritmo nuevo no obliga a tocar el servicio.
 */
public class E1RmStrategyRegistry {

    private final Map<AlgorithmVersion, E1RmStrategy> strategies;

    public E1RmStrategyRegistry(Collection<E1RmStrategy> strategies) {
        if (strategies == null || strategies.isEmpty()) {
            throw new DomainException.Validation("el registro necesita al menos una estrategia");
        }
        Map<AlgorithmVersion, E1RmStrategy> map = new EnumMap<>(AlgorithmVersion.class);
        for (E1RmStrategy strategy : strategies) {
            E1RmStrategy previous = map.put(strategy.version(), strategy);
            if (previous != null) {
                throw new DomainException.Validation(
                        "version de algoritmo duplicada: " + strategy.version());
            }
        }
        this.strategies = Map.copyOf(map);
    }

    public E1RmStrategy get(AlgorithmVersion version) {
        E1RmStrategy strategy = strategies.get(version);
        if (strategy == null) {
            throw new DomainException.Validation(
                    "no hay implementacion registrada para la version " + version);
        }
        return strategy;
    }

    public boolean supports(AlgorithmVersion version) {
        return strategies.containsKey(version);
    }

    public Collection<AlgorithmVersion> versions() {
        return strategies.keySet();
    }
}
