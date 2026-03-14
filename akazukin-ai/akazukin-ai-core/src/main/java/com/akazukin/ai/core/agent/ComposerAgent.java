package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;

@ApplicationScoped
public class ComposerAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT =
            "You are a creative content writer for social media. Create engaging posts based on the given topic and requirements.";

    ComposerAgent() {
    }

    @Inject
    public ComposerAgent(AiTextGenerator aiTextGenerator) {
        super(aiTextGenerator);
    }

    @Override
    public AgentType agentType() {
        return AgentType.COMPOSER;
    }

    @Override
    public String execute(String input) {
        log.log(Level.INFO, "ComposerAgent executing with input length: {0}", input.length());
        return askAi(SYSTEM_PROMPT, input);
    }
}
