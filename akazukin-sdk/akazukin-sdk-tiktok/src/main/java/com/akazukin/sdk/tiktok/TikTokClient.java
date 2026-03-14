package com.akazukin.sdk.tiktok;

import com.akazukin.sdk.tiktok.exception.TikTokApiException;
import com.akazukin.sdk.tiktok.model.PublishResponse;
import com.akazukin.sdk.tiktok.model.PublishStatusResponse;
import com.akazukin.sdk.tiktok.model.TikTokUser;
import com.akazukin.sdk.tiktok.model.TokenResponse;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class TikTokClient implements AutoCloseable {

    private static final String DEFAULT_API_BASE_URL = "https://open.tiktokapis.com/v2";
    private static final String DEFAULT_AUTH_URL = "https://www.tiktok.com/v2/auth/authorize/";
    private static final String DEFAULT_TOKEN_URL = "https://open.tiktokapis.com/v2/oauth/token/";
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

    private final TikTokConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String authBaseUrl;
    private final String tokenUrl;

    private TikTokClient(TikTokConfig config, HttpClient httpClient, ObjectMapper objectMapper,
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
            + "?client_key=" + encode(config.clientKey())
            + "&response_type=code"
            + "&scope=" + encode("user.info.basic,user.info.profile,user.info.stats,video.publish,video.list")
            + "&redirect_uri=" + encode(config.redirectUri())
            + "&state=" + encode(state);
    }

    public TokenResponse exchangeToken(String code) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("client_key", config.clientKey());
        body.put("client_secret", config.clientSecret());
        body.put("code", code);
        body.put("grant_type", "authorization_code");
        body.put("redirect_uri", config.redirectUri());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public TokenResponse refreshToken(String refreshToken) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("client_key", config.clientKey());
        body.put("client_secret", config.clientSecret());
        body.put("refresh_token", refreshToken);
        body.put("grant_type", "refresh_token");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        return parseResponse(response, TokenResponse.class);
    }

    public TikTokUser getMe(String accessToken) {
        String fields = "open_id,display_name,avatar_url,username,follower_count,following_count,likes_count,video_count,is_verified";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/user/info/?fields=" + encode(fields)))
            .header("Authorization", "Bearer " + accessToken)
            .header("Accept", "application/json")
            .GET()
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.path("data").path("user");
        if (data.isMissingNode()) {
            throw new TikTokApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data.user' field", response.body());
        }
        return fromJson(data, TikTokUser.class);
    }

    public PublishResponse initVideoPostByUrl(String accessToken, String videoUrl,
                                              String title, String privacyLevel) {
        Map<String, Object> postInfo = new LinkedHashMap<>();
        postInfo.put("title", title);
        postInfo.put("privacy_level", privacyLevel);

        Map<String, Object> sourceInfo = new LinkedHashMap<>();
        sourceInfo.put("source", "PULL_FROM_URL");
        sourceInfo.put("video_url", videoUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("post_info", postInfo);
        body.put("source_info", sourceInfo);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/post/publish/video/init/"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.path("data");
        if (data.isMissingNode()) {
            throw new TikTokApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, PublishResponse.class);
    }

    public PublishResponse initPhotoPost(String accessToken, String title,
                                         String privacyLevel) {
        Map<String, Object> postInfo = new LinkedHashMap<>();
        postInfo.put("title", title);
        postInfo.put("privacy_level", privacyLevel);

        Map<String, Object> sourceInfo = new LinkedHashMap<>();
        sourceInfo.put("source", "FILE_UPLOAD");
        sourceInfo.put("media_type", "PHOTO");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("post_info", postInfo);
        body.put("source_info", sourceInfo);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/post/publish/content/init/"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.path("data");
        if (data.isMissingNode()) {
            throw new TikTokApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, PublishResponse.class);
    }

    public PublishStatusResponse getPublishStatus(String accessToken, String publishId) {
        Map<String, String> body = Map.of("publish_id", publishId);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiBaseUrl + "/post/publish/status/fetch/"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = sendRequest(request);
        JsonNode root = parseJsonTree(response);
        JsonNode data = root.path("data");
        if (data.isMissingNode()) {
            throw new TikTokApiException(response.statusCode(), "UNKNOWN",
                "Unexpected response: missing 'data' field", response.body());
        }
        return fromJson(data, PublishStatusResponse.class);
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
        } catch (TikTokApiException e) {
            throw e;
        } catch (IOException e) {
            throw new TikTokApiException(0, "IO_ERROR", "Failed to communicate with TikTok API", e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TikTokApiException(0, "INTERRUPTED", "Request was interrupted", e.getMessage(), e);
        }
    }

    private void handleErrorResponse(HttpResponse<String> response) {
        String errorCode = "UNKNOWN";
        String message = "HTTP " + response.statusCode();
        String detail = response.body();

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                errorCode = error.path("code").asText(errorCode);
                message = error.path("message").asText(message);
                detail = error.path("log_id").asText(detail);
            }
        } catch (JsonProcessingException e) {
            throw new TikTokApiException(response.statusCode(), errorCode,
                message + " (response body not valid JSON)", detail, e);
        }

        throw new TikTokApiException(response.statusCode(), errorCode, message, detail);
    }

    private JsonNode parseJsonTree(HttpResponse<String> response) {
        try {
            return objectMapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new TikTokApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", response.body(), e);
        }
    }

    private <T> T parseResponse(HttpResponse<String> response, Class<T> type) {
        try {
            return objectMapper.readValue(response.body(), type);
        } catch (JsonProcessingException e) {
            throw new TikTokApiException(response.statusCode(), "PARSE_ERROR",
                "Failed to parse response JSON", response.body(), e);
        }
    }

    private <T> T fromJson(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new TikTokApiException(0, "PARSE_ERROR",
                "Failed to deserialize JSON node", node.toString(), e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new TikTokApiException(0, "SERIALIZE_ERROR",
                "Failed to serialize request body", e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class Builder {

        private TikTokConfig config;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private String apiBaseUrl;
        private String authBaseUrl;
        private String tokenUrl;

        private Builder() {
        }

        public Builder config(TikTokConfig config) {
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

        public TikTokClient build() {
            if (config == null) {
                throw new IllegalStateException("TikTokConfig must be provided");
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
            return new TikTokClient(config, httpClient, objectMapper, apiBaseUrl, authBaseUrl, tokenUrl);
        }
    }
}
