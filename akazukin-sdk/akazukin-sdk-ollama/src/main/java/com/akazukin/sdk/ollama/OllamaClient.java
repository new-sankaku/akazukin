package com.akazukin.sdk.ollama;

import com.akazukin.sdk.ollama.exception.OllamaApiException;
import com.akazukin.sdk.ollama.model.ChatMessage;
import com.akazukin.sdk.ollama.model.ChatResponse;
import com.akazukin.sdk.ollama.model.GenerateResponse;
import com.akazukin.sdk.ollama.model.ModelInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OllamaClient implements AutoCloseable {

    private static final int HTTP_CLIENT_ERROR = 400;
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OllamaConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration readTimeout;

    private OllamaClient(OllamaConfig config, HttpClient httpClient,
                          ObjectMapper objectMapper, Duration readTimeout) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.readTimeout = readTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getDefaultModel() {
        return config.defaultModel();
    }

    public GenerateResponse generate(String model, String prompt, String system,
                                      double temperature, int maxTokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        if (system != null && !system.isBlank()) {
            body.put("system", system);
        }
        body.put("stream", false);

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", temperature);
        options.put("num_predict", maxTokens);
        body.put("options", options);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/generate"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .timeout(readTimeout)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, GenerateResponse.class);
    }

    public ChatResponse chat(String model, List<ChatMessage> messages,
                              double temperature, int maxTokens) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);

        List<Map<String, String>> messageList = new ArrayList<>();
        for (ChatMessage msg : messages) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("role", msg.role());
            m.put("content", msg.content());
            messageList.add(m);
        }
        body.put("messages", messageList);
        body.put("stream", false);

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", temperature);
        options.put("num_predict", maxTokens);
        body.put("options", options);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/chat"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .timeout(readTimeout)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, ChatResponse.class);
    }

    public List<ModelInfo> listModels() {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/tags"))
            .header("Accept", "application/json")
            .GET()
            .timeout(readTimeout)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode models = root.get("models");
        if (models == null || !models.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(models, new TypeReference<List<ModelInfo>>() {});
    }

    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/api/tags"))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public void close() {
        if (httpClient != null && httpClient != DEFAULT_HTTP_CLIENT) {
            httpClient.close();
        }
    }

    private <T> HttpResponse<T> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        int maxRetries = 3;
        long delayMs = 1000;
        IOException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<T> response = httpClient.send(request, handler);
                if (response.statusCode() == 429 && attempt < maxRetries) {
                    String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
                    long waitMs = retryAfter != null ? Long.parseLong(retryAfter) * 1000 : delayMs;
                    Thread.sleep(Math.min(waitMs, 30000));
                    delayMs *= 2;
                    continue;
                }
                return response;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                }
            }
        }
        throw lastException;
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= HTTP_CLIENT_ERROR) {
                handleErrorResponse(response);
            }
            return response;
        } catch (OllamaApiException e) {
            throw e;
        } catch (IOException e) {
            throw new OllamaApiException(0, "IO_ERROR",
                "Failed to communicate with Ollama API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaApiException(0, "INTERRUPTED",
                "Request was interrupted: " + e.getMessage(), e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        String errorCode = "UNKNOWN";
        String message = "HTTP " + response.statusCode();

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error")) {
                message = root.path("error").asText(message);
                errorCode = "OLLAMA_ERROR";
            }
        } catch (JsonProcessingException e) {
            throw new OllamaApiException(response.statusCode(), errorCode,
                message + " (response body not valid JSON)", e);
        }

        throw new OllamaApiException(response.statusCode(), errorCode, message);
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new OllamaApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new OllamaApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new OllamaApiException(0, "SERIALIZE_ERROR",
                "Failed to serialize request body", e);
        }
    }

    public static class Builder {

        private OllamaConfig config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private Duration readTimeout;

        private Builder() {
        }

        public Builder config(OllamaConfig config) {
            this.config = config;
            return this;
        }

        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public OllamaClient build() {
            if (config == null) {
                throw new IllegalStateException("OllamaConfig must be provided");
            }
            if (httpClient == null) {
                httpClient = DEFAULT_HTTP_CLIENT;
            }
            if (objectMapper == null) {
                objectMapper = DEFAULT_OBJECT_MAPPER;
            }
            if (readTimeout == null) {
                readTimeout = config.timeout();
            }
            return new OllamaClient(config, httpClient, objectMapper, readTimeout);
        }
    }
}
