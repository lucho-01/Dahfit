package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.ClaimCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_codes")
public class JpaClaimCodeEntity {

    @Id
    @Column(name = "user_id")
    UUID userId;
    @Column(name = "code_hash", nullable = false)
    String codeHash;
    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    protected JpaClaimCodeEntity() {
    }

    public static JpaClaimCodeEntity fromDomain(UUID userId, ClaimCode code) {
        JpaClaimCodeEntity entity = new JpaClaimCodeEntity();
        entity.userId = userId;
        entity.codeHash = code.codeHash();
        entity.expiresAt = code.expiresAt();
        return entity;
    }

    public ClaimCode toDomain() {
        return new ClaimCode(codeHash, expiresAt);
    }
}
