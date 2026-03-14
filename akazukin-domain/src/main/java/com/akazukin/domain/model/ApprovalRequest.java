package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class ApprovalRequest {

    private UUID id;
    private UUID postId;
    private UUID requesterId;
    private UUID approverId;
    private UUID teamId;
    private ApprovalAction status;
    private String comment;
    private Instant requestedAt;
    private Instant decidedAt;

    public ApprovalRequest(UUID id, UUID postId, UUID requesterId, UUID approverId, UUID teamId,
                           ApprovalAction status, String comment, Instant requestedAt, Instant decidedAt) {
        this.id = id;
        this.postId = postId;
        this.requesterId = requesterId;
        this.approverId = approverId;
        this.teamId = teamId;
        this.status = status;
        this.comment = comment;
        this.requestedAt = requestedAt;
        this.decidedAt = decidedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public UUID getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(UUID requesterId) {
        this.requesterId = requesterId;
    }

    public UUID getApproverId() {
        return approverId;
    }

    public void setApproverId(UUID approverId) {
        this.approverId = approverId;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public ApprovalAction getStatus() {
        return status;
    }

    public void setStatus(ApprovalAction status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApprovalRequest that = (ApprovalRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ApprovalRequest{" +
                "id=" + id +
                ", postId=" + postId +
                ", requesterId=" + requesterId +
                ", approverId=" + approverId +
                ", teamId=" + teamId +
                ", status=" + status +
                ", requestedAt=" + requestedAt +
                ", decidedAt=" + decidedAt +
                '}';
    }
}
