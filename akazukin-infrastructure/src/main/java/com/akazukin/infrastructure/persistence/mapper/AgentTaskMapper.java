package com.akazukin.infrastructure.persistence.mapper;

import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.infrastructure.persistence.entity.AgentTaskEntity;

public final class AgentTaskMapper {

    private AgentTaskMapper() {
    }

    public static AgentTask toDomain(AgentTaskEntity entity) {
        return new AgentTask(
                entity.id,
                entity.userId,
                AgentType.valueOf(entity.agentType),
                entity.input,
                entity.output,
                entity.status,
                entity.parentTaskId,
                entity.createdAt,
                entity.completedAt
        );
    }

    public static AgentTaskEntity toEntity(AgentTask domain) {
        AgentTaskEntity entity = new AgentTaskEntity();
        entity.id = domain.getId();
        entity.userId = domain.getUserId();
        entity.agentType = domain.getAgentType().name();
        entity.input = domain.getInput();
        entity.output = domain.getOutput();
        entity.status = domain.getStatus();
        entity.parentTaskId = domain.getParentTaskId();
        entity.createdAt = domain.getCreatedAt();
        entity.completedAt = domain.getCompletedAt();
        return entity;
    }
}
