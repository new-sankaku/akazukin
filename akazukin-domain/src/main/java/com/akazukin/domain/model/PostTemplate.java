package com.akazukin.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PostTemplate {

    private UUID id;
    private UUID userId;
    private String name;
    private String content;
    private List<String> placeholders;
    private List<SnsPlatform> platforms;
    private String category;
    private int usageCount;
    private Instant createdAt;
    private Instant updatedAt;

    public PostTemplate(UUID id, UUID userId, String name, String content, List<String> placeholders,
                        List<SnsPlatform> platforms, String category, int usageCount,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.content = content;
        this.placeholders = placeholders;
        this.platforms = platforms;
        this.category = category;
        this.usageCount = usageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getPlaceholders() {
        return placeholders;
    }

    public void setPlaceholders(List<String> placeholders) {
        this.placeholders = placeholders;
    }

    public List<SnsPlatform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<SnsPlatform> platforms) {
        this.platforms = platforms;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostTemplate that = (PostTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PostTemplate{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", usageCount=" + usageCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
