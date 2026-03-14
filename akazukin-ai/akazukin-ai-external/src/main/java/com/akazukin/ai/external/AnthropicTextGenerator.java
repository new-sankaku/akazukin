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

public class AnthropicTextGenerator implements AiTextGenerator {

    private static final Logger LOG = Logger.getLogger(AnthropicTextGenerator.class.getName());
    private static final String DEFAULT_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    private AnthropicTextGenerator(Builder builder) {
        this.httpClient = builder.httpClient;
        this.objectMapper = new ObjectMapper();
        this.apiKey = builder.apiKey;
        this.apiUrl = builder.apiUrl;
        this.model = builder.model;
    }

    @Override
    public AiResponse generate(AiPrompt prompt) {
        long start = System.currentTimeMillis();

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", prompt.maxTokens());

        if (prompt.systemPrompt() != null && !prompt.systemPrompt().isBlank()) {
            requestBody.put("system", prompt.systemPrompt());
        }

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt.userPrompt());
        messages.add(userMessage);
        requestBody.set("messages", messages);

        requestBody.put("temperature", prompt.temperature());

        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Anthropic API returned status " + response.statusCode() + ": " + response.body());
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            String generatedText = extractTextFromResponse(responseJson);
            int inputTokens = responseJson.path("usage").path("input_tokens").asInt(0);
            int outputTokens = responseJson.path("usage").path("output_tokens").asInt(0);
            int tokensUsed = inputTokens + outputTokens;
            long durationMs = System.currentTimeMillis() - start;

            LOG.log(Level.FINE, "Anthropic generation completed in {0}ms, tokens: {1}",
                    new Object[]{durationMs, tokensUsed});

            return new AiResponse(generatedText, tokensUsed, durationMs, model);
        } catch (IOException e) {
            throw new RuntimeException("Failed to communicate with Anthropic API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Anthropic API request was interrupted", e);
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

    private String extractTextFromResponse(JsonNode responseJson) {
        JsonNode contentArray = responseJson.path("content");
        if (contentArray.isArray() && !contentArray.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode block : contentArray) {
                if ("text".equals(block.path("type").asText())) {
                    if (!sb.isEmpty()) {
                        sb.append("\n");
                    }
                    sb.append(block.path("text").asText());
                }
            }
            return sb.toString();
        }
        return "";
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

        public AnthropicTextGenerator build() {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalArgumentException("API key is required");
            }
            if (httpClient == null) {
                httpClient = HttpClient.newHttpClient();
            }
            return new AnthropicTextGenerator(this);
        }
    }
}
