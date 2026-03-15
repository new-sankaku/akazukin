package com.akazukin.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_templates")
public class PostTemplateEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;

    @Column(columnDefinition = "TEXT")
    public String placeholders;

    @Column(columnDefinition = "TEXT")
    public String platforms;

    @Column(length = 50)
    public String category;

    @Column(name = "usage_count", nullable = false)
    public int usageCount;

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
