package com.dafit.domain.service;

import com.dafit.domain.model.Points;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Agregacion MAX (C-09, recomendada).
 *
 * <p>Los puntos del musculo son los de tu <b>mejor</b> prueba elegible. Propiedad
 * clave: es monotona, es decir, registrar un ejercicio nuevo nunca baja la nota.
 *
 * <p>La formula original {@code sum(aporte)/sum(coeficiente)} es una media
 * ponderada: anadir un ejercicio debil hacia bajar la puntuacion y empujaba al
 * usuario a ocultar ejercicios. Ese incentivo perverso es lo que corrige esta
 * estrategia.
 *
 * <p>Con un unico ejercicio evaluado, ambas estrategias dan el mismo resultado,
 * asi que las pruebas de aceptacion del documento siguen siendo validas.
 */
public class MaxPointsAggregation implements MuscleAggregationStrategy {

    public static final String ID = "MAX";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<Points> aggregate(List<MuscleContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return Optional.empty();
        }
        return contributions.stream()
                .map(MuscleContribution::points)
                .max(Comparator.naturalOrder());
    }
}
