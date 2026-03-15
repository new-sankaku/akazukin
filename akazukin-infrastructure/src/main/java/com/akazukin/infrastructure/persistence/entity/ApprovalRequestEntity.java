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
@Table(name = "approval_requests")
public class ApprovalRequestEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "post_id", nullable = false)
    public UUID postId;

    @Column(name = "requester_id", nullable = false)
    public UUID requesterId;

    @Column(name = "approver_id")
    public UUID approverId;

    @Column(name = "team_id")
    public UUID teamId;

    @Column(length = 30)
    public String status;

    @Column(columnDefinition = "TEXT")
    public String comment;

    @Column(name = "requested_at", nullable = false, updatable = false)
    public Instant requestedAt;

    @Column(name = "decided_at")
    public Instant decidedAt;

    @PrePersist
    void prePersist() {
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
    }
}
