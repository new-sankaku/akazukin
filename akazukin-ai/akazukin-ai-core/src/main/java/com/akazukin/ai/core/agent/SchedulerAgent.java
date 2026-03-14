package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;

@ApplicationScoped
public class SchedulerAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT =
            "You are a posting schedule optimizer. Based on engagement data and platform best practices, suggest optimal posting times.";

    SchedulerAgent() {
    }

    @Inject
    public SchedulerAgent(AiTextGenerator aiTextGenerator) {
        super(aiTextGenerator);
    }

    @Override
    public AgentType agentType() {
        return AgentType.SCHEDULER;
    }

    @Override
    public String execute(String input) {
        log.log(Level.INFO, "SchedulerAgent executing with input length: {0}", input.length());
        return askAi(SYSTEM_PROMPT, input);
    }
}
