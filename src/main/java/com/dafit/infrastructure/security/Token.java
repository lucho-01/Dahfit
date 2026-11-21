package com.dafit.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

public class Token {
    private final String value;
    private final UUID userId;
    private final String email;
    private final Instant expiresAt;

    public Token(String value, UUID userId, String email, Instant expiresAt) {
        this.value = value;
        this.userId = userId;
        this.email = email;
        this.expiresAt = expiresAt;
    }

    public String getValue() {
        return value;
    }

    public UUID userId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}