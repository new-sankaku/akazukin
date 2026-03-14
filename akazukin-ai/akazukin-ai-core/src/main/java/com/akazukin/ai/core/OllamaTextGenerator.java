package com.akazukin.ai.core;

import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.port.AiTextGenerator;
import com.akazukin.sdk.ollama.OllamaClient;
import com.akazukin.sdk.ollama.model.GenerateResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class OllamaTextGenerator implements AiTextGenerator {

    private static final Logger LOG = Logger.getLogger(OllamaTextGenerator.class.getName());
    private static final double DEFAULT_PERSONA_TEMPERATURE = 0.7;
    private static final int DEFAULT_PERSONA_MAX_TOKENS = 1024;

    private final OllamaClient ollamaClient;
    private final String defaultModel;

    @Inject
    public OllamaTextGenerator(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.defaultModel = ollamaClient.getDefaultModel();
    }

    @Override
    public AiResponse generate(AiPrompt prompt) {
        long start = System.currentTimeMillis();
        GenerateResponse response = ollamaClient.generate(
            defaultModel,
            prompt.userPrompt(),
            prompt.systemPrompt(),
            prompt.temperature(),
            prompt.maxTokens()
        );
        long durationMs = System.currentTimeMillis() - start;
        LOG.log(Level.FINE, "AI generation completed in {0}ms, tokens: {1}",
            new Object[]{durationMs, response.evalCount()});
        return new AiResponse(response.response(), response.evalCount(), durationMs, response.model());
    }

    @Override
    public AiResponse generateWithPersona(AiPersona persona, String userInput) {
        AiPrompt prompt = new AiPrompt(
            persona.getSystemPrompt(),
            userInput,
            DEFAULT_PERSONA_TEMPERATURE,
            DEFAULT_PERSONA_MAX_TOKENS
        );
        return generate(prompt);
    }
}
