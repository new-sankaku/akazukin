package com.akazukin.ai.ollama;

import com.akazukin.sdk.ollama.OllamaClient;
import com.akazukin.sdk.ollama.OllamaConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class OllamaClientProducer {

    @ConfigProperty(name = "akazukin.ollama.base-url", defaultValue = "http://localhost:11434")
    String baseUrl;

    @ConfigProperty(name = "akazukin.ollama.default-model", defaultValue = "llama3.2")
    String defaultModel;

    @ConfigProperty(name = "akazukin.ollama.timeout-seconds", defaultValue = "30")
    int timeoutSeconds;

    @Produces
    @ApplicationScoped
    public OllamaClient ollamaClient() {
        OllamaConfig config = new OllamaConfig(baseUrl, defaultModel, Duration.ofSeconds(timeoutSeconds));
        return OllamaClient.builder()
            .config(config)
            .build();
    }
}
