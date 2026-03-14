package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;

@ApplicationScoped
public class ComplianceAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT =
            "You are a compliance checker for social media content. Review content for legal, regulatory, and platform policy compliance issues.";

    ComplianceAgent() {
    }

    @Inject
    public ComplianceAgent(AiTextGenerator aiTextGenerator) {
        super(aiTextGenerator);
    }

    @Override
    public AgentType agentType() {
        return AgentType.COMPLIANCE;
    }

    @Override
    public String execute(String input) {
        log.log(Level.INFO, "ComplianceAgent executing with input length: {0}", input.length());
        return askAi(SYSTEM_PROMPT, input);
    }
}
