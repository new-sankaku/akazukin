package com.akazukin.application.usecase;

import com.akazukin.application.dto.AgentPipelineResultDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AgentOrchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentPipelineUseCaseTest {

    private RecordingAgentOrchestrator orchestrator;
    private AgentPipelineUseCase agentPipelineUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        orchestrator = new RecordingAgentOrchestrator();
        agentPipelineUseCase = new AgentPipelineUseCase(orchestrator);
        userId = UUID.randomUUID();
    }

    @Test
    void runPipeline_returnsResultWithAllStages() {
        AgentPipelineResultDto result = agentPipelineUseCase.runPipeline(
                userId, "AI trends", List.of("TWITTER", "BLUESKY"));

        assertNotNull(result);
        assertNotNull(result.analysis());
        assertNotNull(result.content());
        assertNotNull(result.riskAssessment());
        assertNotNull(result.complianceReport());
        assertNotNull(result.scheduleSuggestion());
        assertEquals(2, result.platformContent().size());
        assertEquals("TWITTER", result.platformContent().get(0).platform());
        assertEquals("BLUESKY", result.platformContent().get(1).platform());
    }

    @Test
    void runPipeline_submitsTasksInCorrectOrder() {
        agentPipelineUseCase.runPipeline(userId, "tech news", List.of("TWITTER"));

        assertEquals(AgentType.ANALYST, orchestrator.submittedTasks.get(0).getAgentType());
        assertEquals(AgentType.COMPOSER, orchestrator.submittedTasks.get(1).getAgentType());
        assertEquals(AgentType.TRANSFORMER, orchestrator.submittedTasks.get(2).getAgentType());
        assertEquals(AgentType.SENTINEL, orchestrator.submittedTasks.get(3).getAgentType());
        assertEquals(AgentType.COMPLIANCE, orchestrator.submittedTasks.get(4).getAgentType());
        assertEquals(AgentType.SCHEDULER, orchestrator.submittedTasks.get(5).getAgentType());
    }

    @Test
    void runPipeline_createsTransformTaskPerPlatform() {
        agentPipelineUseCase.runPipeline(userId, "topic", List.of("TWITTER", "BLUESKY", "MASTODON"));

        long transformCount = orchestrator.submittedTasks.stream()
                .filter(t -> t.getAgentType() == AgentType.TRANSFORMER)
                .count();
        assertEquals(3, transformCount);
    }

    @Test
    void runPipeline_throwsWhenTopicIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> agentPipelineUseCase.runPipeline(userId, null, List.of("TWITTER")));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void runPipeline_throwsWhenTopicIsBlank() {
        DomainException exception = assertThrows(DomainException.class,
                () -> agentPipelineUseCase.runPipeline(userId, "  ", List.of("TWITTER")));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void runPipeline_throwsWhenPlatformsIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> agentPipelineUseCase.runPipeline(userId, "topic", null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void runPipeline_throwsWhenPlatformsIsEmpty() {
        DomainException exception = assertThrows(DomainException.class,
                () -> agentPipelineUseCase.runPipeline(userId, "topic", List.of()));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void runPipeline_singlePlatformProducesSinglePlatformContent() {
        AgentPipelineResultDto result = agentPipelineUseCase.runPipeline(
                userId, "single platform", List.of("TWITTER"));

        assertEquals(1, result.platformContent().size());
        assertEquals("TWITTER", result.platformContent().get(0).platform());
    }

    @Test
    void runPipeline_chainsDependenciesBetweenTasks() {
        agentPipelineUseCase.runPipeline(userId, "chaining test", List.of("TWITTER"));

        AgentTask analysisTask = orchestrator.submittedTasks.get(0);
        AgentTask composerTask = orchestrator.submittedTasks.get(1);

        assertEquals(analysisTask.getId(), composerTask.getParentTaskId());

        AgentTask transformTask = orchestrator.submittedTasks.get(2);
        assertEquals(composerTask.getId(), transformTask.getParentTaskId());

        AgentTask sentinelTask = orchestrator.submittedTasks.get(3);
        assertEquals(composerTask.getId(), sentinelTask.getParentTaskId());
    }

    private static class RecordingAgentOrchestrator implements AgentOrchestrator {

        final List<AgentTask> submittedTasks = new ArrayList<>();

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input) {
            return submitTask(userId, agentType, input, null);
        }

        @Override
        public AgentTask submitTask(UUID userId, AgentType agentType, String input, UUID parentTaskId) {
            Instant now = Instant.now();
            AgentTask task = new AgentTask(
                    UUID.randomUUID(), userId, agentType, input,
                    "output-" + agentType.name().toLowerCase(),
                    "COMPLETED", parentTaskId, now, now
            );
            submittedTasks.add(task);
            return task;
        }

        @Override
        public AgentTask getTaskResult(UUID taskId) {
            return submittedTasks.stream()
                    .filter(t -> t.getId().equals(taskId))
                    .findFirst()
                    .orElse(null);
        }
    }
}
