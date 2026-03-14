package com.akazukin.domain.port;

import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;

import java.util.UUID;

public interface AgentOrchestrator {

    AgentTask submitTask(UUID userId, AgentType agentType, String input);

    AgentTask submitTask(UUID userId, AgentType agentType, String input, UUID parentTaskId);

    AgentTask getTaskResult(UUID taskId);
}
