package com.akazukin.sdk.niconico;

import com.akazukin.sdk.niconico.exception.NiconicoApiException;
import com.akazukin.sdk.niconico.model.NiconicoCommentResponse;
import com.akazukin.sdk.niconico.model.NiconicoUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class NiconicoClient implements AutoCloseable {

    private static final String DEFAULT_API_BASE_URL = "https://api.nicovideo.jp/v1";
    private static final int HTTP_CLIENT_ERROR = 400;
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final NiconicoConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;

    private NiconicoClient(NiconicoConfig config, HttpClient httpClient, ObjectMapper objectMapper,
                           String apiBaseUrl) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public NiconicoUser getProfile(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/users/me"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.get("data");
        if (data == null) {
            throw new NiconicoApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, NiconicoUser.class);
    }

    public NiconicoCommentResponse postComment(String accessToken, String videoId, String comment) {
        Map<String, String> payload = Map.of("video_id", videoId, "comment", comment);
        String jsonBody = toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/videos/" + videoId + "/comments"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.get("data");
        if (data == null) {
            throw new NiconicoApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, NiconicoCommentResponse.class);
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
        } catch (NiconicoApiException e) {
            throw e;
        } catch (IOException e) {
            throw new NiconicoApiException(0, "IO_ERROR", "Failed to communicate with Niconico API", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NiconicoApiException(0, "INTERRUPTED", "Request was interrupted", e.getMessage(), e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        String errorCode = "UNKNOWN";
        String message = "HTTP " + response.statusCode();
        String detail = response.body();

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error")) {
                errorCode = root.path("error").asText(errorCode);
                message = root.path("error_description").asText(message);
            }
        } catch (JsonProcessingException e) {
            throw new NiconicoApiException(response.statusCode(), errorCode,
                message + " (response body not valid JSON)", detail, e);
        }

        throw new NiconicoApiException(response.statusCode(), errorCode, message, detail);
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new NiconicoApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", response.body(), e);
        }
    }

    private <T> T fromJson(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new NiconicoApiException(0, "PARSE_ERROR",
                "Failed to deserialize JSON node", node.toString(), e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new NiconicoApiException(0, "SERIALIZE_ERROR",
                "Failed to serialize request body", e.getMessage(), e);
        }
    }

    public static class Builder {

        private NiconicoConfig config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private String apiBaseUrl;

        private Builder() {
        }

        public Builder config(NiconicoConfig config) {
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

        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        public NiconicoClient build() {
            if (config == null) {
                throw new IllegalStateException("NiconicoConfig must be provided");
            }
            if (httpClient == null) {
                httpClient = DEFAULT_HTTP_CLIENT;
            }
            if (objectMapper == null) {
                objectMapper = DEFAULT_OBJECT_MAPPER;
            }
            if (apiBaseUrl == null) {
                apiBaseUrl = DEFAULT_API_BASE_URL;
            }
            return new NiconicoClient(config, httpClient, objectMapper, apiBaseUrl);
        }
    }
}
