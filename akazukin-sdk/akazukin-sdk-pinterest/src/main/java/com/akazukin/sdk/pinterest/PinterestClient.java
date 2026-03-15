package com.akazukin.sdk.pinterest;

import com.akazukin.sdk.pinterest.exception.PinterestApiException;
import com.akazukin.sdk.pinterest.model.Board;
import com.akazukin.sdk.pinterest.model.PinResponse;
import com.akazukin.sdk.pinterest.model.PinterestUser;
import com.akazukin.sdk.pinterest.model.TokenResponse;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PinterestClient implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(PinterestClient.class.getName());

    private static final String DEFAULT_API_BASE_URL = "https://api.pinterest.com/v5";
    private static final String DEFAULT_AUTH_BASE_URL = "https://www.pinterest.com/oauth/";
    private static final String DEFAULT_TOKEN_URL = "https://api.pinterest.com/v5/oauth/token";
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

    private final PinterestConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String authBaseUrl;
    private final String tokenUrl;

    private PinterestClient(PinterestConfig config, HttpClient httpClient, ObjectMapper objectMapper,
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

    public String getAuthorizationUrl(String state, List<String> scopes) {
        String scopeString = String.join(",", scopes);
        return authBaseUrl
            + "?client_id=" + encode(config.appId())
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&response_type=code"
            + "&scope=" + encode(scopeString)
            + "&state=" + encode(state);
    }

    public TokenResponse exchangeToken(String code) {
        long perfStart = System.nanoTime();
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("grant_type", "authorization_code");
            payload.put("code", code);
            payload.put("redirect_uri", config.redirectUri());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + basicAuth())
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, TokenResponse.class);
        } finally {
            perfLog("PinterestClient.exchangeToken", perfStart);
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        long perfStart = System.nanoTime();
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("grant_type", "refresh_token");
            payload.put("refresh_token", refreshToken);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + basicAuth())
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, TokenResponse.class);
        } finally {
            perfLog("PinterestClient.refreshToken", perfStart);
        }
    }

    public PinResponse createPin(String accessToken, String boardId, String title,
                                 String description, String imageUrl) {
        long perfStart = System.nanoTime();
        try {
            Map<String, Object> mediaSource = new HashMap<>();
            mediaSource.put("source_type", "image_url");
            mediaSource.put("url", imageUrl);

            Map<String, Object> payload = new HashMap<>();
            payload.put("board_id", boardId);
            payload.put("title", title);
            payload.put("description", description);
            payload.put("media_source", mediaSource);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/pins"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, PinResponse.class);
        } finally {
            perfLog("PinterestClient.createPin", perfStart);
        }
    }

    public void deletePin(String accessToken, String pinId) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/pins/" + pinId))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .DELETE()
                .timeout(READ_TIMEOUT)
                .build();

            sendRequest(request);
        } finally {
            perfLog("PinterestClient.deletePin", perfStart);
        }
    }

    public PinterestUser getMe(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/user_account"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, PinterestUser.class);
        } finally {
            perfLog("PinterestClient.getMe", perfStart);
        }
    }

    public List<Board> listBoards(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/boards"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            JsonNode root = parseJsonTree(response);
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return List.of();
            }

            List<Board> boards = new ArrayList<>();
            for (JsonNode item : items) {
                boards.add(fromJson(item, Board.class));
            }
            return List.copyOf(boards);
        } finally {
            perfLog("PinterestClient.listBoards", perfStart);
        }
    }

    @Override
    public void close() {
        if (httpClient != null && httpClient != DEFAULT_HTTP_CLIENT) {
            httpClient.close();
        }
    }

    private String basicAuth() {
        String credentials = config.appId() + ":" + config.appSecret();
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
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
            perfLog("PinterestClient.sendWithRetry", perfStart);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= HTTP_CLIENT_ERROR) {
                handleErrorResponse(response);
            }
            return response;
        } catch (PinterestApiException e) {
            throw e;
        } catch (IOException e) {
            throw new PinterestApiException(0, "IO_ERROR",
                "Failed to communicate with Pinterest API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PinterestApiException(0, "INTERRUPTED", "Request was interrupted", e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        String error = "UNKNOWN";
        String message = "HTTP " + response.statusCode();

        try {
            JsonNode root = objectMapper.readTree(response.body());
            error = root.path("code").asText(root.path("error").asText(error));
            message = root.path("message").asText(
                root.path("error_description").asText(message)
            );
        } catch (JsonProcessingException e) {
            throw new PinterestApiException(response.statusCode(), error,
                message + " (response body not valid JSON)", e);
        }

        throw new PinterestApiException(response.statusCode(), error, message, response.body());
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new PinterestApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new PinterestApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    private <T> T fromJson(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new PinterestApiException(0, "PARSE_ERROR",
                "Failed to deserialize JSON node", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new PinterestApiException(0, "SERIALIZE_ERROR",
                "Failed to serialize request body", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class Builder {

        private PinterestConfig config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private String apiBaseUrl;
        private String authBaseUrl;
        private String tokenUrl;

        private Builder() {
        }

        public Builder config(PinterestConfig config) {
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

        public PinterestClient build() {
            if (config == null) {
                throw new IllegalStateException("PinterestConfig must be provided");
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
                authBaseUrl = DEFAULT_AUTH_BASE_URL;
            }
            if (tokenUrl == null) {
                tokenUrl = DEFAULT_TOKEN_URL;
            }
            return new PinterestClient(config, httpClient, objectMapper, apiBaseUrl, authBaseUrl, tokenUrl);
        }
    }
}
