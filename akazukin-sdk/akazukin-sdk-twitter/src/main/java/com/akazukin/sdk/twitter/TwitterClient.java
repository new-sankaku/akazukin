package com.akazukin.sdk.twitter;

import com.akazukin.sdk.twitter.exception.TwitterApiException;
import com.akazukin.sdk.twitter.model.TokenResponse;
import com.akazukin.sdk.twitter.model.TweetResponse;
import com.akazukin.sdk.twitter.model.TwitterUser;
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
import java.util.Map;

public class TwitterClient implements AutoCloseable {

    private static final String API_BASE_URL = "https://api.twitter.com/2";
    private static final String AUTH_URL = "https://twitter.com/i/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.twitter.com/2/oauth2/token";

    private final TwitterConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private TwitterClient(TwitterConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAuthorizationUrl(String state, String codeChallenge) {
        return AUTH_URL
            + "?response_type=code"
            + "&client_id=" + encode(config.clientId())
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&scope=" + encode("tweet.read tweet.write users.read offline.access")
            + "&state=" + encode(state)
            + "&code_challenge=" + encode(codeChallenge)
            + "&code_challenge_method=S256";
    }

    public TokenResponse exchangeToken(String code, String codeVerifier) {
        String body = "grant_type=authorization_code"
            + "&code=" + encode(code)
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&code_verifier=" + encode(codeVerifier)
            + "&client_id=" + encode(config.clientId());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public TokenResponse refreshToken(String refreshToken) {
        String body = "grant_type=refresh_token"
            + "&refresh_token=" + encode(refreshToken)
            + "&client_id=" + encode(config.clientId());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public TweetResponse postTweet(String accessToken, String text) {
        Map<String, String> payload = Map.of("text", text);
        String jsonBody = toJson(payload);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/tweets"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.get("data");
        if (data == null) {
            throw new TwitterApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, TweetResponse.class);
    }

    public void deleteTweet(String accessToken, String tweetId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/tweets/" + tweetId))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .DELETE()
            .timeout(Duration.ofSeconds(10))
            .build();

        sendRequest(request);
    }

    public TwitterUser getMe(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/users/me?user.fields=profile_image_url,public_metrics"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.get("data");
        if (data == null) {
            throw new TwitterApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }

        JsonNode metrics = data.get("public_metrics");
        int followersCount = metrics != null ? metrics.path("followers_count").asInt(0) : 0;
        int followingCount = metrics != null ? metrics.path("following_count").asInt(0) : 0;

        return new TwitterUser(
            data.path("id").asText(),
            data.path("username").asText(),
            data.path("name").asText(),
            data.path("profile_image_url").asText(null),
            followersCount,
            followingCount
        );
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
        } catch (TwitterApiException e) {
            throw e;
        } catch (IOException e) {
            throw new TwitterApiException(0, "IO_ERROR", "Failed to communicate with Twitter API", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TwitterApiException(0, "INTERRUPTED", "Request was interrupted", e.getMessage(), e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        String errorCode = "UNKNOWN";
        String message = "HTTP " + response.statusCode();
        String detail = response.body();

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("errors") && root.get("errors").isArray() && !root.get("errors").isEmpty()) {
                JsonNode firstError = root.get("errors").get(0);
                message = firstError.path("message").asText(message);
                detail = firstError.path("detail").asText(detail);
                errorCode = firstError.path("type").asText(errorCode);
            } else if (root.has("error")) {
                errorCode = root.path("error").asText(errorCode);
                message = root.path("error_description").asText(message);
            }
        } catch (JsonProcessingException ignored) {
            // Use defaults if response body is not valid JSON
        }

        throw new TwitterApiException(response.statusCode(), errorCode, message, detail);
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new TwitterApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", response.body(), e);
        }
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new TwitterApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", response.body(), e);
        }
    }

    private <T> T fromJson(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new TwitterApiException(0, "PARSE_ERROR",
                "Failed to deserialize JSON node", node.toString(), e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new TwitterApiException(0, "SERIALIZE_ERROR",
                "Failed to serialize request body", e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class Builder {

        private TwitterConfig config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        public Builder config(TwitterConfig config) {
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

        public TwitterClient build() {
            if (config == null) {
                throw new IllegalStateException("TwitterConfig must be provided");
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
            return new TwitterClient(config, httpClient, objectMapper);
        }
    }
}
