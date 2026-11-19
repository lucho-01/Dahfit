package com.dafit.infrastructure.adapter.in.rest.controller;

import com.dafit.domain.exception.DomainException;
import com.dafit.infrastructure.adapter.in.rest.dto.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce las excepciones de dominio a codigos HTTP.
 *
 * <p>El dominio no conoce HTTP: este es el unico punto donde un
 * {@code DomainException.NotFound} se convierte en un 404. Mantiene la logica
 * de negocio fuera del protocolo y el protocolo fuera del negocio.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DomainException.Validation.class)
    public ResponseEntity<ErrorResponse> validation(DomainException.Validation ex) {
        return response(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(DomainException.NotFound.class)
    public ResponseEntity<ErrorResponse> notFound(DomainException.NotFound ex) {
        return response(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(DomainException.Forbidden.class)
    public ResponseEntity<ErrorResponse> forbidden(DomainException.Forbidden ex) {
        return response(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(DomainException.Unauthorized.class)
    public ResponseEntity<ErrorResponse> unauthorized(DomainException.Unauthorized ex) {
        return response(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(DomainException.Conflict.class)
    public ResponseEntity<ErrorResponse> conflict(DomainException.Conflict ex) {
        return response(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler({DomainException.NotEligible.class, DomainException.InsufficientData.class})
    public ResponseEntity<ErrorResponse> unprocessable(DomainException ex) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException ex) {
        return response(HttpStatus.BAD_REQUEST, ex);
    }

    /**
     * R-14: una zona horaria o una fecha ISO invalida es un error del cliente,
     * no un 500. Antes {@code ZoneId.of("Bogota/Colombia")} tumbaba la peticion
     * con un stacktrace y sin mensaje util.
     */
    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ErrorResponse> dateTime(DateTimeException ex) {
        return badRequest("fecha o zona horaria invalida: " + ex.getMessage());
    }

    /** Un segmento de ruta que no se puede convertir (por ejemplo un UUID mal formado). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException ex) {
        return badRequest("valor invalido para '" + ex.getName() + "': " + ex.getValue());
    }

    /** JSON mal formado o un enum desconocido en el cuerpo. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return badRequest("el cuerpo de la peticion no se puede leer");
    }

    /**
     * R-14: una restriccion de la base (email duplicado, clave foranea, check)
     * es un conflicto de datos, no un fallo del servidor. Sin este handler,
     * PostgreSQL devolvia 500 y el cliente no sabia que corregir.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataIntegrity(DataIntegrityViolationException ex) {
        // No se devuelve el mensaje del motor: filtra nombres de tablas y SQL.
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "el dato entra en conflicto con una regla de integridad"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.withFields(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "cuerpo de la peticion invalido",
                fields));
    }

    private static ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), message));
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, RuntimeException ex) {
        return ResponseEntity.status(status).body(ErrorResponse.of(
                status.value(), status.getReasonPhrase(), ex.getMessage()));
    }
}
