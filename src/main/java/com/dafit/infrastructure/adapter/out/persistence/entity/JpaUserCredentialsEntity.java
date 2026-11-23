package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.UserCredentials;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class JpaUserCredentialsEntity {

    @Id
    @Column(name = "user_id")
    UUID userId;
    @Column(nullable = false)
    String email;
    @Column(name = "password_hash", nullable = false)
    String passwordHash;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "claimed_at")
    Instant claimedAt;

    protected JpaUserCredentialsEntity() {
    }

    public static JpaUserCredentialsEntity fromDomain(UserCredentials credentials) {
        JpaUserCredentialsEntity entity = new JpaUserCredentialsEntity();
        entity.userId = credentials.userId();
        entity.email = credentials.email();
        entity.passwordHash = credentials.passwordHash();
        entity.createdAt = credentials.createdAt();
        entity.claimedAt = credentials.claimedAt();
        return entity;
    }

    public UserCredentials toDomain() {
        return new UserCredentials(userId, email, passwordHash, createdAt, claimedAt);
    }
}
