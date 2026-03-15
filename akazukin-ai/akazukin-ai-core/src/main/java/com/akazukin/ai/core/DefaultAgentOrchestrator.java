package com.akazukin.ai.core;

import com.akazukin.ai.core.agent.AnalystAgent;
import com.akazukin.ai.core.agent.BaseAgent;
import com.akazukin.ai.core.agent.ComplianceAgent;
import com.akazukin.ai.core.agent.ComposerAgent;
import com.akazukin.ai.core.agent.DirectorAgent;
import com.akazukin.ai.core.agent.SchedulerAgent;
import com.akazukin.ai.core.agent.SentinelAgent;
import com.akazukin.ai.core.agent.TransformerAgent;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.AgentTaskRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class DefaultAgentOrchestrator implements AgentOrchestrator {

    private static final Logger LOG = Logger.getLogger(DefaultAgentOrchestrator.class.getName());

    private final AgentTaskRepository taskRepository;
    private final Map<AgentType, BaseAgent> agents;

    DefaultAgentOrchestrator() {
        this.taskRepository = null;
        this.agents = null;
    }

    @Inject
    public DefaultAgentOrchestrator(AgentTaskRepository taskRepository,
                                     DirectorAgent directorAgent,
                                     AnalystAgent analystAgent,
                                     ComposerAgent composerAgent,
                                     TransformerAgent transformerAgent,
                                     SchedulerAgent schedulerAgent,
                                     SentinelAgent sentinelAgent,
                                     ComplianceAgent complianceAgent) {
        this.taskRepository = taskRepository;
        this.agents = new EnumMap<>(AgentType.class);
        agents.put(AgentType.DIRECTOR, directorAgent);
        agents.put(AgentType.ANALYST, analystAgent);
        agents.put(AgentType.COMPOSER, composerAgent);
        agents.put(AgentType.TRANSFORMER, transformerAgent);
        agents.put(AgentType.SCHEDULER, schedulerAgent);
        agents.put(AgentType.SENTINEL, sentinelAgent);
        agents.put(AgentType.COMPLIANCE, complianceAgent);
    }

    @Override
    public AgentTask submitTask(UUID userId, AgentType agentType, String input) {
        return submitTask(userId, agentType, input, null);
    }

    @Override
    public AgentTask submitTask(UUID userId, AgentType agentType, String input, UUID parentTaskId) {
        AgentTask task = new AgentTask(UUID.randomUUID(), userId, agentType, input, null,
                "PENDING", parentTaskId, Instant.now(), null);
        task = taskRepository.save(task);

        BaseAgent agent = agents.get(agentType);
        if (agent == null) {
            taskRepository.updateStatus(task.getId(), "FAILED", "No agent available for type: " + agentType);
            return taskRepository.findById(task.getId()).orElseThrow();
        }

        taskRepository.updateStatus(task.getId(), "RUNNING", null);
        try {
            String output = agent.execute(input);
            taskRepository.updateStatus(task.getId(), "COMPLETED", output);
            LOG.log(Level.INFO, "Agent task {0} ({1}) completed successfully",
                    new Object[]{task.getId(), agentType});
        } catch (Exception e) {
            String errorMsg = "Agent execution failed: " + e.getMessage();
            taskRepository.updateStatus(task.getId(), "FAILED", errorMsg);
            LOG.log(Level.WARNING, "Agent task {0} ({1}) failed: {2}",
                    new Object[]{task.getId(), agentType, e.getMessage()});
        }

        return taskRepository.findById(task.getId()).orElseThrow();
    }

    @Override
    public AgentTask getTaskResult(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Agent task not found: " + taskId));
    }
}
