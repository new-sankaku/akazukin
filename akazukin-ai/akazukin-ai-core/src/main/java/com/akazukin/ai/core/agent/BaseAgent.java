package com.akazukin.ai.core.agent;

import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.port.AiTextGenerator;

import java.util.logging.Logger;

public abstract class BaseAgent {

    protected final AiTextGenerator aiTextGenerator;
    protected final Logger log;

    protected BaseAgent() {
        this.aiTextGenerator = null;
        this.log = Logger.getLogger(getClass().getName());
    }

    protected BaseAgent(AiTextGenerator aiTextGenerator) {
        this.aiTextGenerator = aiTextGenerator;
        this.log = Logger.getLogger(getClass().getName());
    }

    public abstract AgentType agentType();

    public abstract String execute(String input);

    protected String askAi(String systemPrompt, String userPrompt) {
        AiPrompt prompt = new AiPrompt(systemPrompt, userPrompt, 0.7, 2048);
        AiResponse response = aiTextGenerator.generate(prompt);
        return response.generatedText();
    }
}
