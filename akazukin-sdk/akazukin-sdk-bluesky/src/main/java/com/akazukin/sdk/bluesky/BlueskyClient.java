package com.akazukin.sdk.bluesky;

import com.akazukin.sdk.bluesky.exception.BlueskyApiException;
import com.akazukin.sdk.bluesky.model.PostResponse;
import com.akazukin.sdk.bluesky.model.ProfileResponse;
import com.akazukin.sdk.bluesky.model.SessionResponse;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlueskyClient implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(BlueskyClient.class.getName());

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
        long perfStart = System.nanoTime();
        try {
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
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, SessionResponse.class);
        } finally {
            perfLog("BlueskyClient.createSession", perfStart);
        }
    }

    public SessionResponse refreshSession(String refreshJwt) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(xrpcBaseUrl + "/com.atproto.server.refreshSession"))
                .header("Authorization", "Bearer " + refreshJwt)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, SessionResponse.class);
        } finally {
            perfLog("BlueskyClient.refreshSession", perfStart);
        }
    }

    public PostResponse createPost(String accessJwt, String did, String text) {
        long perfStart = System.nanoTime();
        try {
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
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, PostResponse.class);
        } finally {
            perfLog("BlueskyClient.createPost", perfStart);
        }
    }

    public void deletePost(String accessJwt, String did, String rkey) {
        long perfStart = System.nanoTime();
        try {
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
                .timeout(READ_TIMEOUT)
                .build();

            sendRequest(request);
        } finally {
            perfLog("BlueskyClient.deletePost", perfStart);
        }
    }

    public ProfileResponse getProfile(String accessJwt, String actor) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(xrpcBaseUrl + "/app.bsky.actor.getProfile?actor=" + actor))
                .header("Authorization", "Bearer " + accessJwt)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, ProfileResponse.class);
        } finally {
            perfLog("BlueskyClient.getProfile", perfStart);
        }
    }

    public List<ProfileResponse> getFollowers(String accessJwt, String actor, int limit) {
        long perfStart = System.nanoTime();
        try {
            int maxResults = Math.max(1, Math.min(limit, 100));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(xrpcBaseUrl + "/app.bsky.graph.getFollowers?actor=" + actor
                    + "&limit=" + maxResults))
                .header("Authorization", "Bearer " + accessJwt)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseProfileList(response, "followers");
        } finally {
            perfLog("BlueskyClient.getFollowers", perfStart);
        }
    }

    public List<ProfileResponse> getFollows(String accessJwt, String actor, int limit) {
        long perfStart = System.nanoTime();
        try {
            int maxResults = Math.max(1, Math.min(limit, 100));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(xrpcBaseUrl + "/app.bsky.graph.getFollows?actor=" + actor
                    + "&limit=" + maxResults))
                .header("Authorization", "Bearer " + accessJwt)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseProfileList(response, "follows");
        } finally {
            perfLog("BlueskyClient.getFollows", perfStart);
        }
    }

    private List<ProfileResponse> parseProfileList(HttpResponse<String> response, String arrayField) {
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.get(arrayField);
        if (data == null || !data.isArray()) {
            return List.of();
        }
        List<ProfileResponse> profiles = new ArrayList<>();
        for (JsonNode node : data) {
            profiles.add(new ProfileResponse(
                node.path("did").asText(),
                node.path("handle").asText(),
                node.path("displayName").asText(null),
                node.path("avatar").asText(null),
                node.path("followersCount").asInt(0),
                node.path("followsCount").asInt(0),
                node.path("postsCount").asInt(0)
            ));
        }
        return profiles;
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new BlueskyApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    @Override
    public void close() {
        if (httpClient != null && httpClient != DEFAULT_HTTP_CLIENT) {
            httpClient.close();
        }
    }

    private void perfLog(String methodName, long startNanos) {
        long perfMs = (System.nanoTime() - startNanos) / 1_000_000;
        if (perfMs >= 100) {
            LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{methodName, perfMs});
        } else {
            LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{methodName, perfMs});
        }
    }

    private <T> HttpResponse<T> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        long perfStart = System.nanoTime();
        int maxRetries = 3;
        long delayMs = 1000;
        IOException lastException = null;
        try {
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
        } finally {
            perfLog("BlueskyClient.sendWithRetry", perfStart);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= HTTP_CLIENT_ERROR) {
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
        } catch (JsonProcessingException e) {
            throw new BlueskyApiException(response.statusCode(), error,
                message + " (response body not valid JSON)", e);
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
                httpClient = DEFAULT_HTTP_CLIENT;
            }
            if (objectMapper == null) {
                objectMapper = DEFAULT_OBJECT_MAPPER;
            }
            return new BlueskyClient(config, httpClient, objectMapper);
        }
    }
}
