package com.dafit.domain.port.out;

import com.dafit.domain.model.ClaimCode;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de códigos de reclamación. */
public interface ClaimCodeRepository {
    Optional<ClaimCode> findById(UUID id);

    /**
     * Guarda (o reemplaza) el codigo de reclamacion de un usuario.
     *
     * <p>El {@code userId} es obligatorio: la tabla {@code claim_codes} usa el
     * usuario como clave primaria y sin el no hay forma de asociar el codigo a
     * su cuenta.
     */
    ClaimCode save(UUID userId, ClaimCode claimCode);
    boolean existsById(UUID id);
    ClaimCode findByCode(String code);
    ClaimCode findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}