package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "interactions")
public class InteractionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "sns_account_id", nullable = false)
    public UUID snsAccountId;

    @Column(nullable = false, length = 20)
    public String platform;

    @Column(name = "interaction_type", nullable = false, length = 20)
    public String interactionType;

    @Column(name = "target_post_id", length = 255)
    public String targetPostId;

    @Column(name = "target_user_id", length = 255)
    public String targetUserId;

    @Column(columnDefinition = "TEXT")
    public String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
