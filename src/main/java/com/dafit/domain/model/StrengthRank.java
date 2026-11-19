package com.dafit.domain.model;

/**
 * Etiquetas de rango DAHIAN (§7.3).
 *
 * <p>Son etiquetas internas del producto y NO equivalen a las de Strength Level.
 * La correspondencia exacta con percentiles la define {@link RankScale}.
 *
 * <p><b>C-10 PENDIENTE:</b> con la escala original, "Elite" cubre el percentil
 * 80-95, es decir, una de cada cinco personas. Ver {@link RankScale} para la
 * propuesta de recalibracion.
 */
public enum StrengthRank {
    PRINCIPIANTE,
    NOVATO,
    AVANZADO,
    BESTIA,
    ELITE,
    BRUTAL
}
