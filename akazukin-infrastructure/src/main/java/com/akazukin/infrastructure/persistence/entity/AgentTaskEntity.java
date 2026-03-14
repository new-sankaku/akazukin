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
@Table(name = "agent_tasks")
public class AgentTaskEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(name = "user_id", nullable = false)
    public UUID userId;

    @Column(name = "agent_type", nullable = false, length = 30)
    public String agentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String input;

    @Column(columnDefinition = "TEXT")
    public String output;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "parent_task_id")
    public UUID parentTaskId;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant createdAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
