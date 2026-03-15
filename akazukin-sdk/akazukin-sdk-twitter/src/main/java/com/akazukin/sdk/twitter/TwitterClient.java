package com.akazukin.sdk.twitter;

import com.akazukin.sdk.twitter.exception.TwitterApiException;
import com.akazukin.sdk.twitter.model.TokenResponse;
import com.akazukin.sdk.twitter.model.TweetResponse;
import com.akazukin.sdk.twitter.model.TwitterUser;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwitterClient implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(TwitterClient.class.getName());

    private static final String DEFAULT_API_BASE_URL = "https://api.twitter.com/2";
    private static final String DEFAULT_AUTH_URL = "https://twitter.com/i/oauth2/authorize";
    private static final String DEFAULT_TOKEN_URL = "https://api.twitter.com/2/oauth2/token";
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

    private final TwitterConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String authBaseUrl;
    private final String tokenUrl;

    private TwitterClient(TwitterConfig config, HttpClient httpClient, ObjectMapper objectMapper,
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

    public String getAuthorizationUrl(String state, String codeChallenge) {
        return authBaseUrl
            + "?response_type=code"
            + "&client_id=" + encode(config.clientId())
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&scope=" + encode("tweet.read tweet.write users.read offline.access")
            + "&state=" + encode(state)
            + "&code_challenge=" + encode(codeChallenge)
            + "&code_challenge_method=S256";
    }

    public TokenResponse exchangeToken(String code, String codeVerifier) {
        long perfStart = System.nanoTime();
        try {
            String body = "grant_type=authorization_code"
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(config.redirectUri())
                + "&code_verifier=" + encode(codeVerifier)
                + "&client_id=" + encode(config.clientId());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, TokenResponse.class);
        } finally {
            perfLog("TwitterClient.exchangeToken", perfStart);
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        long perfStart = System.nanoTime();
        try {
            String body = "grant_type=refresh_token"
                + "&refresh_token=" + encode(refreshToken)
                + "&client_id=" + encode(config.clientId());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseResponse(response, TokenResponse.class);
        } finally {
            perfLog("TwitterClient.refreshToken", perfStart);
        }
    }

    public TweetResponse postTweet(String accessToken, String text) {
        long perfStart = System.nanoTime();
        try {
            Map<String, String> payload = Map.of("text", text);
            String jsonBody = toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/tweets"))
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
                throw new TwitterApiException(response.statusCode(), "UNKNOWN",
                    "Unexpected response: missing 'data' field", response.body());
            }
            return fromJson(data, TweetResponse.class);
        } finally {
            perfLog("TwitterClient.postTweet", perfStart);
        }
    }

    public TweetResponse postTweet(String accessToken, String accessTokenSecret, String text) {
        long perfStart = System.nanoTime();
        try {
            String url = apiBaseUrl + "/tweets";
            Map<String, String> payload = Map.of("text", text);
            String jsonBody = toJson(payload);
            OAuth1Signer signer = new OAuth1Signer(config.clientId(), config.clientSecret(),
                accessToken, accessTokenSecret);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", signer.buildAuthorizationHeader("POST", url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            JsonNode root = parseJsonTree(response);
            JsonNode data = root.get("data");
            if (data == null) {
                throw new TwitterApiException(response.statusCode(), "UNKNOWN",
                    "Unexpected response: missing 'data' field", response.body());
            }
            return fromJson(data, TweetResponse.class);
        } finally {
            perfLog("TwitterClient.postTweet(OAuth1)", perfStart);
        }
    }

    public TweetResponse getTweetById(String bearerToken, String tweetId) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/tweets/" + tweetId))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            JsonNode root = parseJsonTree(response);
            JsonNode data = root.get("data");
            if (data == null) {
                throw new TwitterApiException(response.statusCode(), "UNKNOWN",
                    "Unexpected response: missing 'data' field", response.body());
            }
            return fromJson(data, TweetResponse.class);
        } finally {
            perfLog("TwitterClient.getTweetById", perfStart);
        }
    }

    public TwitterUser getUserByUsername(String bearerToken, String username) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/by/username/" + username
                    + "?user.fields=profile_image_url,public_metrics"))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
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
        } finally {
            perfLog("TwitterClient.getUserByUsername", perfStart);
        }
    }

    public void deleteTweet(String accessToken, String tweetId) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/tweets/" + tweetId))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .DELETE()
                .timeout(READ_TIMEOUT)
                .build();

            sendRequest(request);
        } finally {
            perfLog("TwitterClient.deleteTweet", perfStart);
        }
    }

    public void deleteTweet(String accessToken, String accessTokenSecret, String tweetId) {
        long perfStart = System.nanoTime();
        try {
            String url = apiBaseUrl + "/tweets/" + tweetId;
            OAuth1Signer signer = new OAuth1Signer(config.clientId(), config.clientSecret(),
                accessToken, accessTokenSecret);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", signer.buildAuthorizationHeader("DELETE", url))
                .header("Accept", "application/json")
                .DELETE()
                .timeout(READ_TIMEOUT)
                .build();

            sendRequest(request);
        } finally {
            perfLog("TwitterClient.deleteTweet(OAuth1)", perfStart);
        }
    }

    public TwitterUser getMe(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/me?user.fields=profile_image_url,public_metrics"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseMeResponse(response);
        } finally {
            perfLog("TwitterClient.getMe", perfStart);
        }
    }

    public TwitterUser getMe(String accessToken, String accessTokenSecret) {
        long perfStart = System.nanoTime();
        try {
            String url = apiBaseUrl + "/users/me?user.fields=profile_image_url,public_metrics";
            OAuth1Signer signer = new OAuth1Signer(config.clientId(), config.clientSecret(),
                accessToken, accessTokenSecret);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", signer.buildAuthorizationHeader("GET", url))
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = sendRequest(request);
            return parseMeResponse(response);
        } finally {
            perfLog("TwitterClient.getMe(OAuth1)", perfStart);
        }
    }

    private TwitterUser parseMeResponse(HttpResponse<String> response) {
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
            perfLog("TwitterClient.sendWithRetry", perfStart);
        }
    }

    private HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= HTTP_CLIENT_ERROR) {
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
        } catch (JsonProcessingException e) {
            throw new TwitterApiException(response.statusCode(), errorCode,
                message + " (response body not valid JSON)", detail, e);
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
        private String apiBaseUrl;
        private String authBaseUrl;
        private String tokenUrl;

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

        public TwitterClient build() {
            if (config == null) {
                throw new IllegalStateException("TwitterConfig must be provided");
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
            return new TwitterClient(config, httpClient, objectMapper, apiBaseUrl, authBaseUrl, tokenUrl);
        }
    }
}
