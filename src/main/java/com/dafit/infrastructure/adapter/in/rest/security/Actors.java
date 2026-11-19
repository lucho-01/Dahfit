package com.dafit.infrastructure.adapter.in.rest.security;

import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Resuelve el usuario autenticado de la peticion HTTP.
 *
 * <p>{@code null} significa "peticion anonima": con
 * {@code dafit.security.enabled=false} el API esta abierto a proposito y los
 * casos de uso aceptan un actor nulo. Con la seguridad encendida, Spring ya ha
 * rechazado antes cualquier peticion sin token, asi que un actor nulo aqui solo
 * puede venir de un endpoint publico (alta de usuario o de credenciales).
 */
public final class Actors {

    private Actors() {
    }

    public static UUID actorId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof UUID userId ? userId : null;
    }
}
