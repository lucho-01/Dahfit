package com.dafit.domain.port.in;

import java.time.Instant;
import java.util.UUID;

/** Puerto de entrada: login con email y contrasena. */
public interface LoginUseCase {

    /**
     * @throws com.dafit.domain.exception.DomainException.Unauthorized si las
     *         credenciales no son validas
     * @throws com.dafit.domain.exception.DomainException.Forbidden si la cuenta
     *         espera la reclamacion de su duenio (R-02)
     */
    Result login(Command command);

    /**
     * @param claimCode codigo de reclamacion, solo para cuentas pendientes
     */
    record Command(String email, String password, String claimCode) {
    }

    record Result(String token, String tokenType, Instant expiresAt, UUID userId) {

        public Result {
            tokenType = tokenType == null ? "Bearer" : tokenType;
        }
    }
}
