package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AgentTask {

    private UUID id;
    private UUID userId;
    private AgentType agentType;
    private String input;
    private String output;
    private String status;
    private UUID parentTaskId;
    private Instant createdAt;
    private Instant completedAt;

    public AgentTask(UUID id, UUID userId, AgentType agentType, String input, String output,
                     String status, UUID parentTaskId, Instant createdAt, Instant completedAt) {
        this.id = id;
        this.userId = userId;
        this.agentType = agentType;
        this.input = input;
        this.output = output;
        this.status = status;
        this.parentTaskId = parentTaskId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(UUID parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentTask that = (AgentTask) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AgentTask{" +
                "id=" + id +
                ", userId=" + userId +
                ", agentType=" + agentType +
                ", status='" + status + '\'' +
                ", parentTaskId=" + parentTaskId +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }
}
