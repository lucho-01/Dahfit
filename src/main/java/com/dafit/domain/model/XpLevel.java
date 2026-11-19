package com.dafit.domain.model;

import com.dafit.domain.exception.DomainException;

/**
 * Nivel de actividad derivado del XP acumulado.
 *
 * <p>Formula de producto, deliberadamente simple y estable: cada nivel cuesta
 * {@value #XP_PER_LEVEL} XP. El nivel no altera ninguna evaluacion de fuerza.
 */
public record XpLevel(int level, int xpIntoLevel, int xpForNextLevel) {

    public static final int XP_PER_LEVEL = 500;

    public XpLevel {
        if (level < 1) {
            throw new DomainException.Validation("el nivel empieza en 1: " + level);
        }
        if (xpIntoLevel < 0 || xpIntoLevel >= XP_PER_LEVEL) {
            throw new DomainException.Validation("XP dentro del nivel fuera de rango: " + xpIntoLevel);
        }
        if (xpForNextLevel != XP_PER_LEVEL) {
            throw new DomainException.Validation("XP para el siguiente nivel inesperado: " + xpForNextLevel);
        }
    }

    public static XpLevel of(int totalXp) {
        if (totalXp < 0) {
            throw new DomainException.Validation("el XP total no puede ser negativo: " + totalXp);
        }
        return new XpLevel(
                totalXp / XP_PER_LEVEL + 1,
                totalXp % XP_PER_LEVEL,
                XP_PER_LEVEL);
    }
}
