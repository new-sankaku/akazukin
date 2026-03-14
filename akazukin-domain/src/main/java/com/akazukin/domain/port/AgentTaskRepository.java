package com.akazukin.domain.port;

import com.akazukin.domain.model.AgentTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentTaskRepository {

    Optional<AgentTask> findById(UUID id);

    List<AgentTask> findByUserId(UUID userId, int offset, int limit);

    List<AgentTask> findByParentTaskId(UUID parentTaskId);

    AgentTask save(AgentTask task);

    void updateStatus(UUID id, String status, String output);
}
