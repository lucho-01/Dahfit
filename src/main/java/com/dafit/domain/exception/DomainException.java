package com.dafit.domain.exception;

/**
 * Excepciones de negocio. Se declaran como clases anidadas para no llenar el
 * paquete de ficheros de tres lineas.
 *
 * <p>Ninguna de estas excepciones es de infraestructura: el dominio no conoce
 * HTTP. El adaptador de entrada decide el codigo de estado.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    /** Entrada invalida segun las reglas del negocio. */
    public static class Validation extends DomainException {
        public Validation(String message) {
            super(message);
        }
    }

    /** La serie existe pero no puede entrar en el calculo oficial (§7.1). */
    public static class NotEligible extends DomainException {
        public NotEligible(String message) {
            super(message);
        }
    }

    /** No hay datos suficientes para emitir un resultado oficial (C-15). */
    public static class InsufficientData extends DomainException {
        public InsufficientData(String message) {
            super(message);
        }
    }

    /** El agregado solicitado no existe. */
    public static class NotFound extends DomainException {
        public NotFound(String message) {
            super(message);
        }
    }

    /** Operacion no permitida por permisos o consentimiento. */
    public static class Forbidden extends DomainException {
        public Forbidden(String message) {
            super(message);
        }
    }

    /** Falta autenticacion valida (credenciales incorrectas o token ausente). */
    public static class Unauthorized extends DomainException {
        public Unauthorized(String message) {
            super(message);
        }
    }

    /** El recurso ya existe o entra en conflicto con el estado actual. */
    public static class Conflict extends DomainException {
        public Conflict(String message) {
            super(message);
        }
    }
}
