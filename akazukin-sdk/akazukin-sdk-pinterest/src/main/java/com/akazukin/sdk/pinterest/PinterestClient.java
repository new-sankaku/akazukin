package com.akazukin.sdk.pinterest;

import com.akazukin.sdk.pinterest.exception.PinterestApiException;
import com.akazukin.sdk.pinterest.model.Board;
import com.akazukin.sdk.pinterest.model.PinResponse;
import com.akazukin.sdk.pinterest.model.PinterestUser;
import com.akazukin.sdk.pinterest.model.TokenResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
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

public class PinterestClient implements AutoCloseable {

    private static final String API_BASE_URL = "https://api.pinterest.com/v5";
    private static final String AUTH_URL = "https://www.pinterest.com/oauth/";
    private static final String TOKEN_URL = "https://api.pinterest.com/v5/oauth/token";

    private final PinterestConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private PinterestClient(PinterestConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAuthorizationUrl(String state, List<String> scopes) {
        String scopeString = String.join(",", scopes);
        return AUTH_URL
            + "?client_id=" + encode(config.appId())
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&response_type=code"
            + "&scope=" + encode(scopeString)
            + "&state=" + encode(state);
    }

    public TokenResponse exchangeToken(String code) {
        Map<String, String> payload = new HashMap<>();
        payload.put("grant_type", "authorization_code");
        payload.put("code", code);
        payload.put("redirect_uri", config.redirectUri());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Basic " + basicAuth())
            .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public TokenResponse refreshToken(String refreshToken) {
        Map<String, String> payload = new HashMap<>();
        payload.put("grant_type", "refresh_token");
        payload.put("refresh_token", refreshToken);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Basic " + basicAuth())
            .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public PinResponse createPin(String accessToken, String boardId, String title,
                                 String description, String imageUrl) {
        Map<String, Object> mediaSource = new HashMap<>();
        mediaSource.put("source_type", "image_url");
        mediaSource.put("url", imageUrl);

        Map<String, Object> payload = new HashMap<>();
        payload.put("board_id", boardId);
        payload.put("title", title);
        payload.put("description", description);
        payload.put("media_source", mediaSource);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/pins"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, PinResponse.class);
    }

    public void deletePin(String accessToken, String pinId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/pins/" + pinId))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .DELETE()
            .timeout(Duration.ofSeconds(10))
            .build();

        sendRequest(request);
    }

    public PinterestUser getMe(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/user_account"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, PinterestUser.class);
    }

    public List<Board> listBoards(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/boards"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(10))
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
    }

    @Override
    public void close() {
        httpClient.close();
    }

    private String basicAuth() {
        String credentials = config.appId() + ":" + config.appSecret();
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
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
        } catch (JsonProcessingException ignored) {
            // Use defaults if response body is not valid JSON
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

        public PinterestClient build() {
            if (config == null) {
                throw new IllegalStateException("PinterestConfig must be provided");
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
            return new PinterestClient(config, httpClient, objectMapper);
        }
    }
}
