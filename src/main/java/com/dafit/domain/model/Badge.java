package com.dafit.domain.model;

/** Insignias del MVP. Criterios en {@code BadgeEvaluator}. */
public enum Badge {

    PRIMER_ENTRENAMIENTO("Primer entrenamiento"),
    RACHA_4("Racha de 4 dias"),
    DIEZ_ENTRENAMIENTOS("10 entrenamientos");

    private final String label;

    Badge(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
