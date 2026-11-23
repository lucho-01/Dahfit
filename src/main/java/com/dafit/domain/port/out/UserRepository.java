package com.dafit.domain.port.out;

import com.dafit.domain.model.User;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: repositorio de usuarios. */
public interface UserRepository {
    Optional<User> findById(UUID id);
    User save(User user);
    boolean existsById(UUID id);
    Optional<User> findByDisplayName(String displayName);
}