package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;

@ApplicationScoped
public class DirectorAgent extends BaseAgent {

    private static final String SYSTEM_PROMPT =
            "You are a social media strategy director. Given the user's goals and context, create a publishing plan.";

    DirectorAgent() {
    }

    @Inject
    public DirectorAgent(AiTextGenerator aiTextGenerator) {
        super(aiTextGenerator);
    }

    @Override
    public AgentType agentType() {
        return AgentType.DIRECTOR;
    }

    @Override
    public String execute(String input) {
        log.log(Level.INFO, "DirectorAgent executing with input length: {0}", input.length());
        return askAi(SYSTEM_PROMPT, input);
    }
}
