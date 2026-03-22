package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ab_tests")
public class ABTestEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(name = "variant_a", nullable = false, columnDefinition = "TEXT")
    public String variantA;

    @Column(name = "variant_b", nullable = false, columnDefinition = "TEXT")
    public String variantB;

    @Column(name = "variant_c", columnDefinition = "TEXT")
    public String variantC;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "platforms", length = 500)
    public String platforms;

    @Column(name = "started_at")
    public Instant startedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "winner_variant", length = 10)
    public String winnerVariant;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
