package com.akazukin.application.usecase;

import com.akazukin.application.dto.AgentPipelineResultDto;
import com.akazukin.application.dto.PlatformContentDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AgentTask;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AgentOrchestrator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AgentPipelineUseCase {

    private static final Logger LOG = Logger.getLogger(AgentPipelineUseCase.class.getName());

    private final AgentOrchestrator orchestrator;

    @Inject
    public AgentPipelineUseCase(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public AgentPipelineResultDto runPipeline(UUID userId, String topic, List<String> targetPlatforms) {
        long perfStart = System.nanoTime();
        try {
            if (topic == null || topic.isBlank()) {
                throw new DomainException("INVALID_INPUT", "Topic is required");
            }
            if (targetPlatforms == null || targetPlatforms.isEmpty()) {
                throw new DomainException("INVALID_INPUT", "At least one target platform is required");
            }

            LOG.log(Level.INFO, "Starting agent pipeline for user {0}, topic: {1}, platforms: {2}",
                    new Object[]{userId, topic, targetPlatforms});

            AgentTask analysis = orchestrator.submitTask(userId, AgentType.ANALYST,
                    "Analyze trends and engagement patterns for topic: " + topic);

            AgentTask content = orchestrator.submitTask(userId, AgentType.COMPOSER,
                    "Create a social media post about: " + topic + "\nAnalysis: " + analysis.getOutput(),
                    analysis.getId());

            List<AgentTask> transforms = new ArrayList<>();
            for (String platform : targetPlatforms) {
                AgentTask transform = orchestrator.submitTask(userId, AgentType.TRANSFORMER,
                        "Adapt for " + platform + ": " + content.getOutput(), content.getId());
                transforms.add(transform);
            }

            AgentTask sentinelCheck = orchestrator.submitTask(userId, AgentType.SENTINEL,
                    "Check for risks: " + content.getOutput(), content.getId());

            AgentTask complianceCheck = orchestrator.submitTask(userId, AgentType.COMPLIANCE,
                    "Check compliance: " + content.getOutput(), content.getId());

            AgentTask schedule = orchestrator.submitTask(userId, AgentType.SCHEDULER,
                    "Suggest optimal posting time for platforms: " + String.join(", ", targetPlatforms),
                    content.getId());

            List<PlatformContentDto> platformContent = new ArrayList<>();
            for (int i = 0; i < targetPlatforms.size(); i++) {
                platformContent.add(new PlatformContentDto(
                        targetPlatforms.get(i),
                        transforms.get(i).getOutput()
                ));
            }

            LOG.log(Level.INFO, "Agent pipeline completed for user {0}", userId);

            return new AgentPipelineResultDto(
                    analysis.getOutput(),
                    content.getOutput(),
                    platformContent,
                    sentinelCheck.getOutput(),
                    complianceCheck.getOutput(),
                    schedule.getOutput()
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AgentPipelineUseCase.runPipeline", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AgentPipelineUseCase.runPipeline", perfMs});
            }
        }
    }
}
