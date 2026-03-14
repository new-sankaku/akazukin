package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Interaction {

    private UUID id;
    private UUID userId;
    private UUID snsAccountId;
    private SnsPlatform platform;
    private InteractionType interactionType;
    private String targetPostId;
    private String targetUserId;
    private String content;
    private Instant createdAt;

    public Interaction(UUID id, UUID userId, UUID snsAccountId, SnsPlatform platform,
                       InteractionType interactionType, String targetPostId, String targetUserId,
                       String content, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.snsAccountId = snsAccountId;
        this.platform = platform;
        this.interactionType = interactionType;
        this.targetPostId = targetPostId;
        this.targetUserId = targetUserId;
        this.content = content;
        this.createdAt = createdAt;
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

    public UUID getSnsAccountId() {
        return snsAccountId;
    }

    public void setSnsAccountId(UUID snsAccountId) {
        this.snsAccountId = snsAccountId;
    }

    public SnsPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SnsPlatform platform) {
        this.platform = platform;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public String getTargetPostId() {
        return targetPostId;
    }

    public void setTargetPostId(String targetPostId) {
        this.targetPostId = targetPostId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Interaction that = (Interaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Interaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", snsAccountId=" + snsAccountId +
                ", platform=" + platform +
                ", interactionType=" + interactionType +
                ", targetPostId='" + targetPostId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
