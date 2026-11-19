package com.dafit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DAFIT - seguimiento de entrenamiento, rangos de fuerza y gamificacion.
 *
 * <p><b>Arquitectura hexagonal (puertos y adaptadores), un solo hexagono global.</b>
 *
 * <pre>
 * com.dafit
 * |
 * +-- domain/                      Nucleo del negocio. Java puro: sin Spring,
 * |   |                            sin JPA, sin Jackson.
 * |   +-- model/                   Entidades y objetos de valor
 * |   +-- exception/               Excepciones de negocio
 * |   +-- port/
 * |   |   +-- in/                  Puertos de entrada (casos de uso)
 * |   |   +-- out/                 Puertos de salida (BD, APIs externas)
 * |   +-- service/                 Servicios de dominio (logica pura)
 * |
 * +-- application/
 * |   +-- usecase/                 Implementacion de los puertos de entrada
 * |
 * +-- infrastructure/              Detalles tecnologicos
 *     +-- adapter/
 *     |   +-- in/rest/
 *     |   |   +-- controller/
 *     |   |   +-- dto/
 *     |   |   +-- mapper/
 *     |   +-- out/persistence/
 *     |       +-- entity/
 *     |       +-- repository/
 *     |       +-- adapter/
 *     |       +-- mapper/
 *     +-- config/                  Beans de Spring y configuracion
 * </pre>
 *
 * <p><b>Dependencias permitidas:</b>
 * <pre>
 *   infrastructure  ->  application  ->  domain
 *   infrastructure  ->  domain
 *   application     ->  domain
 *   domain          ->  nada
 * </pre>
 *
 * <p>La regla no se sostiene por disciplina: la verifica
 * {@code HexagonalArchitectureTest} con ArchUnit antes de cada build.
 */
@SpringBootApplication
public class DafitApplication {

    public static void main(String[] args) {
        SpringApplication.run(DafitApplication.class, args);
    }
}
