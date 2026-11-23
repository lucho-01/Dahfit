package com.dafit.domain.port.out;

import com.dafit.domain.model.ClaimCode;
import com.dafit.domain.model.UserCredentials;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de credenciales de usuario. */
public interface CredentialsRepository {
    Optional<UserCredentials> findById(UUID id);
    UserCredentials save(UserCredentials credentials);
    boolean existsById(UUID id);
    Optional<UserCredentials> findByUserId(UUID userId);
    Optional<UserCredentials> findByClaimCode(String claimCode);
    Optional<UserCredentials> findByEmail(String email);
}