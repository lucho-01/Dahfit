package com.dafit.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI de la API.
 *
 * <p>springdoc lo genera a partir de los propios controllers y DTOs, de modo
 * que el contrato no se puede desincronizar del codigo. El frontend lo consume
 * en {@code /v3/api-docs} y hay una UI de exploracion en
 * {@code /swagger-ui.html}.
 *
 * <p>Los DTO son {@code record} con Bean Validation: la descripcion de campos y
 * los limites salen del codigo, no de un documento aparte.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "DAFIT API",
        version = "0.1.0",
        description = """
                Seguimiento de entrenamiento, rangos de fuerza y gamificacion.

                Notas de contrato:
                - Los errores de dominio se traducen a 400/403/404/422 con el
                  mismo cuerpo ErrorResponse.
                - GET /api/users/{userId}/workouts/current devuelve 204 cuando
                  el usuario no tiene una sesion abierta.
                - Las evaluaciones pueden llegar con estado SIN_FUENTE: es el
                  resultado correcto mientras no haya tablas de percentiles con
                  licencia cargadas.
                """,
        contact = @Contact(name = "DAFIT")))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {
}
