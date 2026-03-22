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
@Table(name = "approval_rules")
public class ApprovalRuleEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "team_id", nullable = false)
    public UUID teamId;

    @Column(length = 20, nullable = false)
    public String role;

    @Column(name = "post_approval_required", nullable = false)
    public boolean postApprovalRequired;

    @Column(name = "schedule_approval_required", nullable = false)
    public boolean scheduleApprovalRequired;

    @Column(name = "media_approval_required", nullable = false)
    public boolean mediaApprovalRequired;

    @Column(name = "ai_check_required", nullable = false)
    public boolean aiCheckRequired;

    @Column(name = "ai_auto_reject", nullable = false)
    public boolean aiAutoReject;

    @Column(name = "min_approvers", nullable = false)
    public int minApprovers;

    @Column(name = "approval_deadline_hours", nullable = false)
    public int approvalDeadlineHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
