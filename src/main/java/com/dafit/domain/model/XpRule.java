package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

/**
 * Regla de puntuacion de XP: cuanto vale cada tipo de evento.
 *
 * <p>Los valores llegan de configuracion ({@code dafit.gamification.*}), no
 * estan hardcodeados en el calculo. Los topes diario y semanal se aplican
 * despues, en {@code XpCalculator}.
 */
public record XpRule(XpEventType type, int points) {

    public XpRule {
        if (type == null) {
            throw new DomainException.Validation("el tipo de evento es obligatorio");
        }
        if (points < 0) {
            throw new DomainException.Validation("el XP de la regla no puede ser negativo: " + points);
        }
    }
}
