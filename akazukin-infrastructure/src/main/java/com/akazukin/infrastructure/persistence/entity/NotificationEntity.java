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
@Table(name = "notifications")
public class NotificationEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(nullable = false, length = 30)
    public String type;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(columnDefinition = "TEXT")
    public String body;

    @Column(name = "related_entity_id")
    public UUID relatedEntityId;

    @Column(name = "is_read", nullable = false)
    public boolean isRead;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
