package com.akazukin.ai.core;

import com.akazukin.domain.model.TranslationRequest;
import com.akazukin.domain.model.TranslationResult;
import com.akazukin.domain.port.AiTranslator;
import com.akazukin.sdk.ollama.OllamaClient;
import com.akazukin.sdk.ollama.model.GenerateResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class OllamaTranslator implements AiTranslator {

    private static final Logger LOG = Logger.getLogger(OllamaTranslator.class.getName());
    private static final double TRANSLATION_TEMPERATURE = 0.3;
    private static final int TRANSLATION_MAX_TOKENS = 2048;

    private final OllamaClient ollamaClient;
    private final String defaultModel;

    @Inject
    public OllamaTranslator(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.defaultModel = ollamaClient.getDefaultModel();
    }

    @Override
    public TranslationResult translate(TranslationRequest request) {
        String systemPrompt = String.format(
                "You are a professional translator. Translate the following text from %s to %s. "
                        + "Output ONLY the translated text, nothing else.",
                request.sourceLang(),
                request.targetLang()
        );

        long start = System.currentTimeMillis();
        GenerateResponse response = ollamaClient.generate(
                defaultModel,
                request.sourceText(),
                systemPrompt,
                TRANSLATION_TEMPERATURE,
                TRANSLATION_MAX_TOKENS
        );
        long durationMs = System.currentTimeMillis() - start;

        LOG.log(Level.FINE, "Translation completed in {0}ms, tokens: {1}",
                new Object[]{durationMs, response.evalCount()});

        return new TranslationResult(
                response.response().strip(),
                request.sourceLang(),
                request.targetLang(),
                1.0
        );
    }
}
