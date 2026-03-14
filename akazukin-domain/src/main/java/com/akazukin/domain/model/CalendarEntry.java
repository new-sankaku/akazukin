package com.akazukin.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CalendarEntry {

    private UUID id;
    private UUID userId;
    private UUID postId;
    private String title;
    private String description;
    private Instant scheduledAt;
    private List<SnsPlatform> platforms;
    private String color;
    private Instant createdAt;
    private Instant updatedAt;

    public CalendarEntry(UUID id, UUID userId, UUID postId, String title, String description,
                         Instant scheduledAt, List<SnsPlatform> platforms, String color,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.postId = postId;
        this.title = title;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.platforms = platforms;
        this.color = color;
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

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public List<SnsPlatform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<SnsPlatform> platforms) {
        this.platforms = platforms;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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
        CalendarEntry that = (CalendarEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
