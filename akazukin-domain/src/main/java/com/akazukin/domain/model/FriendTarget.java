package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class FriendTarget {

    private UUID id;
    private UUID userId;
    private SnsPlatform platform;
    private String targetIdentifier;
    private String displayName;
    private String notes;
    private Instant createdAt;

    public FriendTarget(UUID id, UUID userId, SnsPlatform platform, String targetIdentifier,
                        String displayName, String notes, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.targetIdentifier = targetIdentifier;
        this.displayName = displayName;
        this.notes = notes;
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

    public SnsPlatform getPlatform() {
        return platform;
    }

    public void setPlatform(SnsPlatform platform) {
        this.platform = platform;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
        FriendTarget that = (FriendTarget) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "FriendTarget{" +
                "id=" + id +
                ", userId=" + userId +
                ", platform=" + platform +
                ", targetIdentifier='" + targetIdentifier + '\'' +
                ", displayName='" + displayName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
