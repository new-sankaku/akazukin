package com.akazukin.domain.port;

import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface AgentTaskRepository {

    Optional<AgentTask> findById(UUID id);

    List<AgentTask> findByUserId(UUID userId, int offset, int limit);

    List<AgentTask> findByParentTaskId(UUID parentTaskId);

    AgentTask save(AgentTask task);

    void updateStatus(UUID id, String status, String output);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, String status);

    List<AgentTask> findByUserIdOrderByCreatedAt(UUID userId, int offset, int limit);

    Map<AgentType, Long> countByUserIdGroupByAgentType(UUID userId);

    Map<AgentType, Long> countByUserIdAndStatusGroupByAgentType(UUID userId, String status);

    List<AgentTask> findByUserIdAndCreatedAtAfter(UUID userId, Instant after);
}
