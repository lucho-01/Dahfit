package com.dafit.domain.model;

import com.dafit.domain.model.Sex;

/**
 * Categoria de referencia de una tabla de percentiles (§7.2).
 *
 * <p>La interpolacion solo es valida dentro de la MISMA categoria. Mezclar
 * categorias produce comparaciones sin sentido.
 *
 * @param ageBand banda de edad opcional; muchas fuentes no la publican.
 */
public record BenchmarkCategory(Sex sex, String ageBand) {

    public BenchmarkCategory {
        sex = sex == null ? Sex.UNSPECIFIED : sex;
        ageBand = ageBand == null || ageBand.isBlank() ? null : ageBand;
    }

    public static BenchmarkCategory of(Sex sex) {
        return new BenchmarkCategory(sex, null);
    }

    public boolean isUsable() {
        return sex != null && sex != Sex.UNSPECIFIED;
    }

    public boolean matches(BenchmarkCategory other) {
        if (other == null) {
            return false;
        }
        boolean sameAge = ageBand == null ? other.ageBand == null : ageBand.equals(other.ageBand);
        return sex == other.sex && sameAge;
    }
}
