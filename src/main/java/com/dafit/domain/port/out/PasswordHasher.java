package com.dafit.domain.port.out;

/** Puerto de salida: servicio de hashing de contraseñas. */
public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String passwordHash);
}