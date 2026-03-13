package com.akazukin.sdk.reddit;

import com.akazukin.sdk.reddit.exception.RedditApiException;
import com.akazukin.sdk.reddit.model.RedditUser;
import com.akazukin.sdk.reddit.model.SubmitResponse;
import com.akazukin.sdk.reddit.model.TokenResponse;
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
import java.util.Base64;
import java.util.List;

public class RedditClient implements AutoCloseable {

    private static final String API_BASE_URL = "https://oauth.reddit.com";
    private static final String AUTH_URL = "https://www.reddit.com/api/v1/authorize";
    private static final String TOKEN_URL = "https://www.reddit.com/api/v1/access_token";
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

    private final RedditConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private RedditClient(RedditConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
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
            + "?client_id=" + encode(config.clientId())
            + "&response_type=code"
            + "&state=" + encode(state)
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&duration=permanent"
            + "&scope=" + encode(scopeString);
    }

    public TokenResponse exchangeToken(String code) {
        String body = "grant_type=authorization_code"
            + "&code=" + encode(code)
            + "&redirect_uri=" + encode(config.redirectUri());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("Authorization", "Basic " + basicAuth())
            .header("User-Agent", config.userAgent())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public TokenResponse refreshToken(String refreshToken) {
        String body = "grant_type=refresh_token"
            + "&refresh_token=" + encode(refreshToken);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("Authorization", "Basic " + basicAuth())
            .header("User-Agent", config.userAgent())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public SubmitResponse submitPost(String accessToken, String subreddit, String title, String text) {
        String body = "api_type=json"
            + "&kind=self"
            + "&sr=" + encode(subreddit)
            + "&title=" + encode(title)
            + "&text=" + encode(text);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/api/submit"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("User-Agent", config.userAgent())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);

        JsonNode json = root.path("json");
        JsonNode errors = json.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            String errorCode = errors.get(0).get(0).asText("UNKNOWN");
            String errorMessage = errors.get(0).size() > 1 ? errors.get(0).get(1).asText("") : "";
            throw new RedditApiException(response.statusCode(), errorCode, errorMessage, response.body());
        }

        JsonNode data = json.path("data");
        return new SubmitResponse(
            data.path("id").asText(null),
            data.path("name").asText(null),
            data.path("url").asText(null)
        );
    }

    public void deletePost(String accessToken, String fullname) {
        String body = "id=" + encode(fullname);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/api/del"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("User-Agent", config.userAgent())
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();

        sendRequest(request);
    }

    public RedditUser getMe(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE_URL + "/api/v1/me"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .header("User-Agent", config.userAgent())
            .GET()
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, RedditUser.class);
    }

    @Override
    public void close() {
        if (httpClient != null && httpClient != DEFAULT_HTTP_CLIENT) {
            httpClient.close();
        }
    }

    private String basicAuth() {
        String credentials = config.clientId() + ":" +
            (config.clientSecret() != null ? config.clientSecret() : "");
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
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
        } catch (RedditApiException e) {
            throw e;
        } catch (IOException e) {
            throw new RedditApiException(0, "IO_ERROR", "Failed to communicate with Reddit API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedditApiException(0, "INTERRUPTED", "Request was interrupted", e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        String error = "UNKNOWN";
        String message = "HTTP " + response.statusCode();

        try {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error")) {
                error = root.path("error").asText(error);
                message = root.path("message").asText(
                    root.path("error_description").asText(message)
                );
            }
        } catch (JsonProcessingException e) {
            throw new RedditApiException(response.statusCode(), error,
                message + " (response body not valid JSON)", e);
        }

        throw new RedditApiException(response.statusCode(), error, message, response.body());
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new RedditApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new RedditApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class Builder {

        private RedditConfig config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;

        private Builder() {
        }

        public Builder config(RedditConfig config) {
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

        public RedditClient build() {
            if (config == null) {
                throw new IllegalStateException("RedditConfig must be provided");
            }
            if (httpClient == null) {
                httpClient = DEFAULT_HTTP_CLIENT;
            }
            if (objectMapper == null) {
                objectMapper = DEFAULT_OBJECT_MAPPER;
            }
            return new RedditClient(config, httpClient, objectMapper);
        }
    }
}
