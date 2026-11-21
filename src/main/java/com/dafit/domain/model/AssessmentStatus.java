package com.dafit.domain.model;

/** Estado de una evaluacion (C-15). */
/**
 * <p>Regla dura del producto: <b>sin dato no es cero puntos</b>. Cada motivo de
 * ausencia tiene su propio estado para que la interfaz pueda explicarlo en vez
 * de mostrar un 0 enganoso.</p>
 */
public enum AssessmentStatus {

    /** Hay datos suficientes y el resultado es oficial. */
    OK,

    /** No hay ninguna serie elegible para ese ejercicio. */
    SIN_DATOS,

    /** No hay pesada corporal en la ventana (C-05). */
    FALTA_PESO_CORPORAL,

    /** No hay sexo/categoria de referencia (§4). */
    FALTA_CATEGORIA,

    /** No hay tabla de referencia para ese ejercicio (§7.2). */
    SIN_FUENTE,

    /** Ejercicio sin estándares registrados (DEC-05). */
    NO_DISPONIBLE,

    /** Por debajo del percentil minimo de la tabla. */
    FUERA_DE_TABLA_P5,

    /** Por encima del percentil maximo de la tabla. */
    FUERA_DE_TABLA_P95,

    /** No se alcanzan las pruebas/cobertura minimas del musculo (C-14). */
    COBERTURA_INSUFICIENTE;

    public boolean isOfficial() {
        return this == OK;
    }

    public boolean isMissingData() {
        return this == SIN_DATOS || this == FALTA_PESO_CORPORAL
                || this == FALTA_CATEGORIA || this == SIN_FUENTE || this == NO_DISPONIBLE;
    }

    public boolean isOutOfTable() {
        return this == FUERA_DE_TABLA_P5 || this == FUERA_DE_TABLA_P95;
    }
}