package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sns_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "platform"})
})
public class SnsAccountEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    public UserEntity user;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(nullable = false, length = 20)
    public String platform;

    @Column(name = "account_identifier", nullable = false, length = 255)
    public String accountIdentifier;

    @Column(name = "display_name", length = 255)
    public String displayName;

    @Column(name = "access_token", columnDefinition = "TEXT")
    public String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    public String refreshToken;

    @Column(name = "token_expires_at")
    public Instant tokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
