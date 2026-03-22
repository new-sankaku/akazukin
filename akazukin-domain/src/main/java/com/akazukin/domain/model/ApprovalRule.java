package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ApprovalRule {

    private UUID id;
    private UUID teamId;
    private Role role;
    private boolean postApprovalRequired;
    private boolean scheduleApprovalRequired;
    private boolean mediaApprovalRequired;
    private boolean aiCheckRequired;
    private boolean aiAutoReject;
    private int minApprovers;
    private int approvalDeadlineHours;
    private Instant createdAt;
    private Instant updatedAt;

    public ApprovalRule(UUID id, UUID teamId, Role role,
                        boolean postApprovalRequired, boolean scheduleApprovalRequired,
                        boolean mediaApprovalRequired, boolean aiCheckRequired,
                        boolean aiAutoReject, int minApprovers, int approvalDeadlineHours,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.teamId = teamId;
        this.role = role;
        this.postApprovalRequired = postApprovalRequired;
        this.scheduleApprovalRequired = scheduleApprovalRequired;
        this.mediaApprovalRequired = mediaApprovalRequired;
        this.aiCheckRequired = aiCheckRequired;
        this.aiAutoReject = aiAutoReject;
        this.minApprovers = minApprovers;
        this.approvalDeadlineHours = approvalDeadlineHours;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isPostApprovalRequired() { return postApprovalRequired; }
    public void setPostApprovalRequired(boolean v) { this.postApprovalRequired = v; }
    public boolean isScheduleApprovalRequired() { return scheduleApprovalRequired; }
    public void setScheduleApprovalRequired(boolean v) { this.scheduleApprovalRequired = v; }
    public boolean isMediaApprovalRequired() { return mediaApprovalRequired; }
    public void setMediaApprovalRequired(boolean v) { this.mediaApprovalRequired = v; }
    public boolean isAiCheckRequired() { return aiCheckRequired; }
    public void setAiCheckRequired(boolean v) { this.aiCheckRequired = v; }
    public boolean isAiAutoReject() { return aiAutoReject; }
    public void setAiAutoReject(boolean v) { this.aiAutoReject = v; }
    public int getMinApprovers() { return minApprovers; }
    public void setMinApprovers(int v) { this.minApprovers = v; }
    public int getApprovalDeadlineHours() { return approvalDeadlineHours; }
    public void setApprovalDeadlineHours(int v) { this.approvalDeadlineHours = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalRule that = (ApprovalRule) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
