package com.dafit.domain.port.in;

import com.dafit.domain.model.User;

import java.time.LocalDate;
import java.util.UUID;

/** Puerto de entrada: consultar el perfil de un usuario (§6). */
public interface GetUserUseCase {

    /**
     * @param actorId usuario autenticado ({@code null} si el API esta abierto)
     * @throws com.dafit.domain.exception.DomainException.NotFound    si no existe
     * @throws com.dafit.domain.exception.DomainException.Forbidden   si el actor no es el usuario ni ADMIN
     */
    User byId(UUID actorId, UUID userId);

    /** "Hoy" en la zona horaria del usuario: base de los cortes diarios de gamificacion. */
    LocalDate today(UUID userId);
}
