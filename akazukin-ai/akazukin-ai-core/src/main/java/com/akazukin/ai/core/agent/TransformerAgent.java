package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;

@ApplicationScoped
public class TransformerAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT =
            "You are a content adaptation specialist. Transform the given text to fit the specified social media platform's character limits, tone, and best practices.";

    TransformerAgent() {
    }

    @Inject
    public TransformerAgent(AiTextGenerator aiTextGenerator) {
        super(aiTextGenerator);
    }

    @Override
    public AgentType agentType() {
        return AgentType.TRANSFORMER;
    }

    @Override
    public String execute(String input) {
        log.log(Level.INFO, "TransformerAgent executing with input length: {0}", input.length());
        return askAi(SYSTEM_PROMPT, input);
    }
}
