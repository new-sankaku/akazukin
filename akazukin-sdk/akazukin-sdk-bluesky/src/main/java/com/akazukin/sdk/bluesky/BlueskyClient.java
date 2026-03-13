package com.akazukin.sdk.bluesky;

import com.akazukin.sdk.bluesky.exception.BlueskyApiException;
import com.akazukin.sdk.bluesky.model.PostResponse;
import com.akazukin.sdk.bluesky.model.ProfileResponse;
import com.akazukin.sdk.bluesky.model.SessionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class BlueskyClient implements AutoCloseable {

    private final BlueskyConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String xrpcBaseUrl;

    private BlueskyClient(BlueskyConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.xrpcBaseUrl = config.serviceUrl() + "/xrpc";
    }

    public static Builder builder() {
        return new Builder();
    }

    public SessionResponse createSession(String identifier, String password) {
        Map<String, String> payload = Map.of(
            "identifier", identifier,
            "password", password
        );
        String jsonBody = toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(xrpcBaseUrl + "/com.atproto.server.createSession"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, SessionResponse.class);
    }

    public SessionResponse refreshSession(String refreshJwt) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(xrpcBaseUrl + "/com.atproto.server.refreshSession"))
            .header("Authorization", "Bearer " + refreshJwt)
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, SessionResponse.class);
    }

    public PostResponse createPost(String accessJwt, String did, String text) {
        String createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC));

        Map<String, Object> record = Map.of(
            "$type", "app.bsky.feed.post",
            "text", text,
            "createdAt", createdAt
        );

        Map<String, Object> payload = Map.of(
            "repo", did,
            "collection", "app.bsky.feed.post",
            "record", record
        );

        String jsonBody = toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(xrpcBaseUrl + "/com.atproto.repo.createRecord"))
            .header("Authorization", "Bearer " + accessJwt)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, PostResponse.class);
    }

    public void deletePost(String accessJwt, String did, String rkey) {
        Map<String, String> payload = Map.of(
            "repo", did,
            "collection", "app.bsky.feed.post",
            "rkey", rkey
        );
        String jsonBody = toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(xrpcBaseUrl + "/com.atproto.repo.deleteRecord"))
            .header("Authorization", "Bearer " + accessJwt)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(10))
            .build();

        sendRequest(request);
    }

    public ProfileResponse getProfile(String accessJwt, String actor) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(xrpcBaseUrl + "/app.bsky.actor.getProfile?actor=" + actor))
            .header("Authorization", "Bearer " + accessJwt)
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, ProfileResponse.class);
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                handleErrorResponse(response);
            }
            return response;
        } catch (BlueskyApiException e) {
            throw e;
        } catch (IOException e) {
            throw new BlueskyApiException(0, "IO_ERROR", "Failed to communicate with Bluesky API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BlueskyApiException(0, "INTERRUPTED", "Request was interrupted", e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        String error = "UNKNOWN";
        String message = "HTTP " + response.statusCode();

        try {
            JsonNode root = objectMapper.readTree(response.body());
            error = root.path("error").asText(error);
            message = root.path("message").asText(message);
        } catch (JsonProcessingException ignored) {
            // Use defaults if response body is not valid JSON
        }

        throw new BlueskyApiException(response.statusCode(), error, message, response.body());
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new BlueskyApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BlueskyApiException(0, "SERIALIZE_ERROR", "Failed to serialize request body", e);
        }
    }

    public static class Builder {

        private BlueskyConfig config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        public Builder config(BlueskyConfig config) {
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

        public BlueskyClient build() {
            if (config == null) {
                throw new IllegalStateException("BlueskyConfig must be provided");
            }
            if (httpClient == null) {
                httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            }
            if (objectMapper == null) {
                objectMapper = new ObjectMapper();
            }
            return new BlueskyClient(config, httpClient, objectMapper);
        }
    }
}
