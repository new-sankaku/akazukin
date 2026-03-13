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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_targets")
public class PostTargetEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, insertable = false, updatable = false)
    public PostEntity post;

    @Column(name = "post_id", nullable = false)
    public UUID postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sns_account_id", nullable = false, insertable = false, updatable = false)
    public SnsAccountEntity snsAccount;

    @Column(name = "sns_account_id", nullable = false)
    public UUID snsAccountId;

    @Column(nullable = false, length = 20)
    public String platform;

    @Column(name = "platform_post_id", length = 255)
    public String platformPostId;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;

    @Column(name = "published_at")
    public Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
