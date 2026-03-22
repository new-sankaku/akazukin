package com.akazukin.ai.core;

import com.akazukin.domain.port.AiInfrastructurePort;
import com.akazukin.sdk.ollama.OllamaClient;
import com.akazukin.sdk.ollama.model.ModelInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class OllamaInfrastructureAdapter implements AiInfrastructurePort {

    private static final Logger LOG = Logger.getLogger(OllamaInfrastructureAdapter.class.getName());

    private final OllamaClient ollamaClient;

    @Inject
    public OllamaInfrastructureAdapter(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    @Override
    public boolean isOllamaAvailable() {
        return ollamaClient.isAvailable();
    }

    @Override
    public String getOllamaEndpoint() {
        try {
            var field = ollamaClient.getClass().getDeclaredField("config");
            field.setAccessible(true);
            var config = field.get(ollamaClient);
            var method = config.getClass().getMethod("baseUrl");
            return (String) method.invoke(config);
        } catch (Exception e) {
            LOG.log(Level.FINE, "Could not extract Ollama endpoint", e);
            return "http://localhost:11434";
        }
    }

    @Override
    public String getOllamaDefaultModel() {
        return ollamaClient.getDefaultModel();
    }

    @Override
    public List<String> listOllamaModels() {
        try {
            return ollamaClient.listModels().stream()
                    .map(ModelInfo::name)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to list Ollama models: {0}", e.getMessage());
            return List.of();
        }
    }
}
