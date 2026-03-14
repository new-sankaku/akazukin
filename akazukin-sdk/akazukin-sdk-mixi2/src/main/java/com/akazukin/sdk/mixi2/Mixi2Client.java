package com.akazukin.sdk.mixi2;

import com.akazukin.sdk.mixi2.exception.Mixi2ApiException;
import com.akazukin.sdk.mixi2.model.Mixi2PostResponse;
import com.akazukin.sdk.mixi2.model.Mixi2TokenResponse;
import com.akazukin.sdk.mixi2.model.Mixi2User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

public class Mixi2Client implements AutoCloseable {

    private static final String DEFAULT_API_BASE_URL = "https://api.mixi.io/v2";
    private static final String DEFAULT_AUTH_URL = "https://mixi.io/connect/authorize";
    private static final String DEFAULT_TOKEN_URL = "https://api.mixi.io/v2/oauth2/token";
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

    private final Mixi2Config config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String authBaseUrl;
    private final String tokenUrl;

    private Mixi2Client(Mixi2Config config, HttpClient httpClient, ObjectMapper objectMapper,
                        String apiBaseUrl, String authBaseUrl, String tokenUrl) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiBaseUrl = apiBaseUrl;
        this.authBaseUrl = authBaseUrl;
        this.tokenUrl = tokenUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAuthorizationUrl(String state) {
        return authBaseUrl
            + "?response_type=code"
            + "&client_id=" + encode(config.clientId())
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&scope=" + encode("profile post")
            + "&state=" + encode(state);
    }

    public Mixi2TokenResponse exchangeToken(String code, String redirectUri) {
        String body = "grant_type=authorization_code"
            + "&code=" + encode(code)
            + "&redirect_uri=" + encode(redirectUri)
            + "&client_id=" + encode(config.clientId())
            + "&client_secret=" + encode(config.clientSecret());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, Mixi2TokenResponse.class);
    }

    public Mixi2TokenResponse refreshToken(String refreshToken) {
        String body = "grant_type=refresh_token"
            + "&refresh_token=" + encode(refreshToken)
            + "&client_id=" + encode(config.clientId())
            + "&client_secret=" + encode(config.clientSecret());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, Mixi2TokenResponse.class);
    }

    public Mixi2User getProfile(String accessToken) {
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
            throw new Mixi2ApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, Mixi2User.class);
    }

    public Mixi2PostResponse post(String accessToken, String text) {
        Map<String, String> payload = Map.of("text", text);
        String jsonBody = toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/posts"))
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
            throw new Mixi2ApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, Mixi2PostResponse.class);
    }

    public void deletePost(String accessToken, String postId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/posts/" + postId))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .DELETE()
            .timeout(READ_TIMEOUT)
            .build();

        sendRequest(request);
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
        } catch (Mixi2ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new Mixi2ApiException(0, "IO_ERROR", "Failed to communicate with Mixi2 API", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Mixi2ApiException(0, "INTERRUPTED", "Request was interrupted", e.getMessage(), e);
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
            throw new Mixi2ApiException(response.statusCode(), errorCode,
                message + " (response body not valid JSON)", detail, e);
        }

        throw new Mixi2ApiException(response.statusCode(), errorCode, message, detail);
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new Mixi2ApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", response.body(), e);
        }
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new Mixi2ApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", response.body(), e);
        }
    }

    private <T> T fromJson(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new Mixi2ApiException(0, "PARSE_ERROR",
                "Failed to deserialize JSON node", node.toString(), e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new Mixi2ApiException(0, "SERIALIZE_ERROR",
                "Failed to serialize request body", e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class Builder {

        private Mixi2Config config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private String apiBaseUrl;
        private String authBaseUrl;
        private String tokenUrl;

        private Builder() {
        }

        public Builder config(Mixi2Config config) {
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

        public Builder authBaseUrl(String authBaseUrl) {
            this.authBaseUrl = authBaseUrl;
            return this;
        }

        public Builder tokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
            return this;
        }

        public Mixi2Client build() {
            if (config == null) {
                throw new IllegalStateException("Mixi2Config must be provided");
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
            if (authBaseUrl == null) {
                authBaseUrl = DEFAULT_AUTH_URL;
            }
            if (tokenUrl == null) {
                tokenUrl = DEFAULT_TOKEN_URL;
            }
            return new Mixi2Client(config, httpClient, objectMapper, apiBaseUrl, authBaseUrl, tokenUrl);
        }
    }
}
