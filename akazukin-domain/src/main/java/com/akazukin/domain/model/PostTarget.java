package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PostTarget {

    private UUID id;
    private UUID postId;
    private UUID snsAccountId;
    private SnsPlatform platform;
    private String platformPostId;
    private PostStatus status;
    private String errorMessage;
    private Instant publishedAt;
    private Instant createdAt;

    public PostTarget(UUID id, UUID postId, UUID snsAccountId, SnsPlatform platform,
                      String platformPostId, PostStatus status, String errorMessage,
                      Instant publishedAt, Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.snsAccountId = snsAccountId;
        this.platform = platform;
        this.platformPostId = platformPostId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
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

    public String getPlatformPostId() {
        return platformPostId;
    }

    public void setPlatformPostId(String platformPostId) {
        this.platformPostId = platformPostId;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSuccessful() {
        return this.status == PostStatus.PUBLISHED;
    }

    public boolean isFailed() {
        return this.status == PostStatus.FAILED;
    }

    public void markAsPublished(String platformPostId) {
        if (platformPostId == null || platformPostId.isBlank()) {
            throw new IllegalArgumentException("Platform post ID must not be null or blank");
        }
        this.status = PostStatus.PUBLISHED;
        this.platformPostId = platformPostId;
        this.publishedAt = Instant.now();
    }

    public void markAsFailed(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException("Error message must not be null or blank");
        }
        this.status = PostStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostTarget that = (PostTarget) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PostTarget{" +
                "id=" + id +
                ", postId=" + postId +
                ", snsAccountId=" + snsAccountId +
                ", platform=" + platform +
                ", platformPostId='" + platformPostId + '\'' +
                ", status=" + status +
                ", errorMessage='" + errorMessage + '\'' +
                ", publishedAt=" + publishedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
