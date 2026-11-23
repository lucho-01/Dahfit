package com.dafit.infrastructure.adapter.out.persistence.entity;

import com.dafit.domain.model.Kg;
import com.dafit.domain.model.Sex;
import com.dafit.domain.model.User;
import com.dafit.domain.model.UserRole;
import com.dafit.domain.model.WeightUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "users")
public class JpaUserEntity {

    @Id
    UUID id;

    @Column(name = "display_name", nullable = false)
    String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Sex sex;

    @Column(name = "birth_date")
    LocalDate birthDate;

    @Column(name = "bodyweight_kg")
    java.math.BigDecimal bodyweightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_unit", nullable = false)
    WeightUnit preferredUnit;

    @Column(name = "time_zone", nullable = false)
    String timeZone;

    @Column(name = "benchmark_consent", nullable = false)
    boolean benchmarkConsent;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected JpaUserEntity() {
    }

    public static JpaUserEntity fromDomain(User user) {
        JpaUserEntity entity = new JpaUserEntity();
        entity.id = user.id();
        entity.displayName = user.displayName();
        entity.role = user.role();
        entity.sex = user.sex();
        entity.birthDate = user.birthDate();
        entity.bodyweightKg = user.bodyweightKg() == null ? null : user.bodyweightKg().value();
        entity.preferredUnit = user.preferredUnit();
        entity.timeZone = user.timeZone().getId();
        entity.benchmarkConsent = user.benchmarkConsent();
        entity.createdAt = user.createdAt();
        return entity;
    }

    public User toDomain() {
        return new User(id, displayName, role, sex, birthDate,
                bodyweightKg == null ? null : Kg.ofKg(bodyweightKg),
                preferredUnit, ZoneId.of(timeZone), benchmarkConsent, createdAt);
    }
}
