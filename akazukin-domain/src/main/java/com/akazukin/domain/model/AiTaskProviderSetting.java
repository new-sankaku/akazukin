package com.akazukin.domain.model;

import java.util.Objects;
import java.util.UUID;

public class AiTaskProviderSetting {

    private UUID id;
    private UUID userId;
    private AiTaskType taskType;
    private AiModelProvider provider;

    public AiTaskProviderSetting(UUID id, UUID userId, AiTaskType taskType, AiModelProvider provider) {
        this.id = id;
        this.userId = userId;
        this.taskType = taskType;
        this.provider = provider;
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

    public AiTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(AiTaskType taskType) {
        this.taskType = taskType;
    }

    public AiModelProvider getProvider() {
        return provider;
    }

    public void setProvider(AiModelProvider provider) {
        this.provider = provider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiTaskProviderSetting that = (AiTaskProviderSetting) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
