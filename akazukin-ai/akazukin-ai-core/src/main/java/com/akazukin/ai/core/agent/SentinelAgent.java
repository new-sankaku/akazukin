package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;

@ApplicationScoped
public class SentinelAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT =
            "You are a social media crisis monitor. Analyze the given content or situation for potential reputation risks, inappropriate content, or viral negative sentiment.";

    SentinelAgent() {
    }

    @Inject
    public SentinelAgent(AiTextGenerator aiTextGenerator) {
        super(aiTextGenerator);
    }

    @Override
    public AgentType agentType() {
        return AgentType.SENTINEL;
    }

    @Override
    public String execute(String input) {
        log.log(Level.INFO, "SentinelAgent executing with input length: {0}", input.length());
        return askAi(SYSTEM_PROMPT, input);
    }
}
