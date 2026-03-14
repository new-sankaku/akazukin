package com.akazukin.ai.external;

import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.port.AiTextGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenAiTextGenerator implements AiTextGenerator {

    private static final Logger LOG = Logger.getLogger(OpenAiTextGenerator.class.getName());
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    private OpenAiTextGenerator(Builder builder) {
        this.httpClient = builder.httpClient;
        this.objectMapper = new ObjectMapper();
        this.apiKey = builder.apiKey;
        this.apiUrl = builder.apiUrl;
        this.model = builder.model;
    }

    @Override
    public AiResponse generate(AiPrompt prompt) {
        long start = System.currentTimeMillis();

        ArrayNode messages = objectMapper.createArrayNode();
        if (prompt.systemPrompt() != null && !prompt.systemPrompt().isBlank()) {
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", prompt.systemPrompt());
            messages.add(systemMessage);
        }
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt.userPrompt());
        messages.add(userMessage);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.set("messages", messages);
        requestBody.put("temperature", prompt.temperature());
        requestBody.put("max_tokens", prompt.maxTokens());

        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            String generatedText = responseJson.path("choices").path(0).path("message").path("content").asText();
            int tokensUsed = responseJson.path("usage").path("total_tokens").asInt(0);
            long durationMs = System.currentTimeMillis() - start;

            LOG.log(Level.FINE, "OpenAI generation completed in {0}ms, tokens: {1}",
                    new Object[]{durationMs, tokensUsed});

            return new AiResponse(generatedText, tokensUsed, durationMs, model);
        } catch (IOException e) {
            throw new RuntimeException("Failed to communicate with OpenAI API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("OpenAI API request was interrupted", e);
        }
    }

    @Override
    public AiResponse generateWithPersona(AiPersona persona, String userInput) {
        AiPrompt prompt = new AiPrompt(
                persona.getSystemPrompt(),
                userInput,
                0.7,
                1024
        );
        return generate(prompt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private HttpClient httpClient;
        private String apiKey;
        private String apiUrl = DEFAULT_API_URL;
        private String model = DEFAULT_MODEL;

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public OpenAiTextGenerator build() {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("API key is required");
            }
            if (httpClient == null) {
                httpClient = HttpClient.newHttpClient();
            }
            return new OpenAiTextGenerator(this);
        }
    }
}
