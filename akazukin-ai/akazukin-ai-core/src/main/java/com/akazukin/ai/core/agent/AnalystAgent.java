package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;

@ApplicationScoped
public class AnalystAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT =
            "You are a social media analyst. Analyze engagement data and trends to provide actionable insights.";

    AnalystAgent() {
    }

    @Inject
    public AnalystAgent(AiTextGenerator aiTextGenerator) {
        super(aiTextGenerator);
    }

    @Override
    public AgentType agentType() {
        return AgentType.ANALYST;
    }

    @Override
    public String execute(String input) {
        log.log(Level.INFO, "AnalystAgent executing with input length: {0}", input.length());
        return askAi(SYSTEM_PROMPT, input);
    }
}
