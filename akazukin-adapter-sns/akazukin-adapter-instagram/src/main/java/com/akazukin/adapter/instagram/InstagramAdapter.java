package com.akazukin.adapter.instagram;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

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
import java.time.Instant;
import java.util.Objects;

public class InstagramAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v19.0";
    private static final String AUTH_URL = "https://www.facebook.com/v19.0/dialog/oauth";
    private static final String TOKEN_URL = "https://graph.facebook.com/v19.0/oauth/access_token";

    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile String cachedUserId;

    public InstagramAdapter(String clientId, String clientSecret,
                            HttpClient httpClient, ObjectMapper objectMapper) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public InstagramAdapter(String clientId, String clientSecret) {
        this(clientId, clientSecret, SHARED_HTTP_CLIENT, SHARED_OBJECT_MAPPER);
    }

    public InstagramAdapter() {
        this(
            System.getProperty("akazukin.instagram.client-id",
                System.getenv().getOrDefault("INSTAGRAM_CLIENT_ID", "")),
            System.getProperty("akazukin.instagram.client-secret",
                System.getenv().getOrDefault("INSTAGRAM_CLIENT_SECRET", ""))
        );
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.INSTAGRAM;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        try {
            checkRateLimit();
            String url = AUTH_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(callbackUrl)
                + "&scope=" + encode("instagram_basic,instagram_content_publish")
                + "&response_type=code"
                + "&state=" + encode(state);
            recordApiCall();
            return url;
        } catch (RuntimeException e) {
            throw wrapException("getAuthorizationUrl", e);
        }
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            String url = TOKEN_URL
                + "?client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + encode(callbackUrl)
                + "&code=" + encode(code);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "exchangeToken");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            int expiresIn = json.path("expires_in").asInt(0);
            Instant expiresAt = expiresIn > 0 ? Instant.now().plusSeconds(expiresIn) : null;

            return new SnsAuthToken(
                json.path("access_token").asText(),
                null,
                expiresAt,
                null
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("exchangeToken", e);
        } catch (RuntimeException e) {
            throw wrapException("exchangeToken", e);
        }
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        try {
            checkRateLimit();
            String url = GRAPH_API_BASE + "/oauth/access_token"
                + "?grant_type=fb_exchange_token"
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&fb_exchange_token=" + encode(refreshToken);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "refreshToken");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            int expiresIn = json.path("expires_in").asInt(0);
            Instant expiresAt = expiresIn > 0 ? Instant.now().plusSeconds(expiresIn) : null;

            return new SnsAuthToken(
                json.path("access_token").asText(),
                null,
                expiresAt,
                null
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("refreshToken", e);
        } catch (RuntimeException e) {
            throw wrapException("refreshToken", e);
        }
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        try {
            checkRateLimit();
            String url = GRAPH_API_BASE + "/me"
                + "?fields=id,username,name,profile_picture_url,followers_count"
                + "&access_token=" + encode(accessToken);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getProfile");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return new SnsProfile(
                json.path("username").asText(),
                json.path("name").asText(""),
                json.path("profile_picture_url").asText(null),
                json.path("followers_count").asInt(0)
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("getProfile", e);
        } catch (RuntimeException e) {
            throw wrapException("getProfile", e);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        try {
            checkRateLimit();
            String userId = getUserId(accessToken);

            if (request.mediaUrls().isEmpty()) {
                throw new IllegalArgumentException(
                    "Instagram requires at least one media URL. Text-only posts are not supported.");
            }

            String imageUrl = request.mediaUrls().get(0);

            // Step 1: Create media container
            String createBody = "image_url=" + encode(imageUrl)
                + "&caption=" + encode(request.content())
                + "&access_token=" + encode(accessToken);

            HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_API_BASE + "/" + userId + "/media"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createBody))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> createResponse = httpClient.send(
                createRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(createResponse, "post (create container)");
            JsonNode createJson = objectMapper.readTree(createResponse.body());
            String containerId = createJson.path("id").asText();

            // Step 2: Publish
            String publishBody = "creation_id=" + encode(containerId)
                + "&access_token=" + encode(accessToken);

            HttpRequest publishRequest = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_API_BASE + "/" + userId + "/media_publish"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(publishBody))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> publishResponse = httpClient.send(
                publishRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(publishResponse, "post (publish)");
            JsonNode publishJson = objectMapper.readTree(publishResponse.body());
            recordApiCall();

            String postId = publishJson.path("id").asText();
            String postUrl = "https://www.instagram.com/p/" + postId;

            return new PostResult(postId, postUrl, Instant.now());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("post", e);
        } catch (RuntimeException e) {
            throw wrapException("post", e);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        throw new UnsupportedOperationException(
            "Instagram Graph API does not support deleting posts programmatically. "
                + "Posts can only be deleted through the Instagram app or website."
        );
    }

    private String getUserId(String accessToken) throws IOException, InterruptedException {
        if (cachedUserId != null) {
            return cachedUserId;
        }
        String url = GRAPH_API_BASE + "/me?fields=id&access_token=" + encode(accessToken);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponseStatus(response, "getUserId");
        JsonNode json = objectMapper.readTree(response.body());
        String userId = json.path("id").asText();
        cachedUserId = userId;
        return userId;
    }

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
