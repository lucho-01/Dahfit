package com.dafit.domain.model;

/**
 * Semantica de la carga registrada (§7.1 - "mostrar claramente si el peso
 * registrado es por mano o total").
 *
 * <p><b>C-08:</b> los modos de peso corporal son imprescindibles: sin ellos el
 * rango de espalda (dominadas) no se puede calcular, y con el tampoco el global.
 */
public enum LoadMode {

    /** Barra: el numero ya es el total. */
    TOTAL,

    /** Mancuernas: el numero es por mano; el total exige el numero de implementos. */
    PER_HAND,

    /** Unilateral con maquina o polea: el numero es por lado. */
    PER_SIDE,

    /** Peso corporal puro (dominada, fondo, flexion): la metrica oficial es repeticiones. */
    BODYWEIGHT,

    /** Peso corporal + lastre (cinturon, chaleco). */
    BODYWEIGHT_ADDED,

    /** Peso corporal asistido (banda, maquina asistida). */
    ASSISTED,

    /** Pila de maquina: el numero es la carga marcada. */
    MACHINE_STACK,

    /** Banda elastica: carga no cuantificable de forma fiable. */
    BAND;

    public boolean isBodyweightBased() {
        return this == BODYWEIGHT || this == BODYWEIGHT_ADDED || this == ASSISTED;
    }

    /** Modos en los que la metrica natural de referencia son repeticiones, no kilos. */
    public boolean usesRepsAsMetric() {
        return this == BODYWEIGHT || this == BAND;
    }
}
