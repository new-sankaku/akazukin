package com.akazukin.adapter.threads;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsPostStats;
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
import java.util.Optional;

public class ThreadsAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final int THREADS_MAX_LENGTH = 500;

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String GRAPH_API_BASE = "https://graph.threads.net/v1.0";
    private static final String AUTH_URL = "https://threads.net/oauth/authorize";
    private static final String TOKEN_URL = "https://graph.threads.net/oauth/access_token";

    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private volatile String cachedUserId;

    public ThreadsAdapter(String clientId, String clientSecret,
                          HttpClient httpClient, ObjectMapper objectMapper) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public ThreadsAdapter(String clientId, String clientSecret) {
        this(clientId, clientSecret, SHARED_HTTP_CLIENT, SHARED_OBJECT_MAPPER);
    }

    public ThreadsAdapter() {
        this(
            System.getProperty("akazukin.threads.client-id",
                System.getenv().getOrDefault("THREADS_CLIENT_ID", "")),
            System.getProperty("akazukin.threads.client-secret",
                System.getenv().getOrDefault("THREADS_CLIENT_SECRET", ""))
        );
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.THREADS;
    }

    @Override
    public int getMaxContentLength() {
        return THREADS_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return AUTH_URL
            + "?client_id=" + encode(clientId)
            + "&redirect_uri=" + encode(callbackUrl)
            + "&scope=" + encode("threads_basic,threads_content_publish")
            + "&response_type=code"
            + "&state=" + encode(state);
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            String body = "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + encode(callbackUrl)
                + "&code=" + encode(code);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "exchangeToken");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return new SnsAuthToken(
                json.path("access_token").asText(),
                null,
                null,
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
            String url = GRAPH_API_BASE + "/refresh_access_token"
                + "?grant_type=th_refresh_token"
                + "&access_token=" + encode(refreshToken);

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
        SnsProfile cached = getCachedProfile(accessToken);
        if (cached != null) {
            return cached;
        }
        try {
            checkRateLimit();
            String url = GRAPH_API_BASE + "/me"
                + "?fields=id,username,name,threads_profile_picture_url"
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

            SnsProfile profile = new SnsProfile(
                json.path("username").asText(),
                json.path("name").asText(""),
                json.path("threads_profile_picture_url").asText(null),
                0
            );
            cacheProfile(accessToken, profile);
            return profile;
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

            // Step 1: Create media container
            String createBody = "media_type=TEXT"
                + "&text=" + encode(request.content())
                + "&access_token=" + encode(accessToken);

            HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH_API_BASE + "/" + userId + "/threads"))
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
                .uri(URI.create(GRAPH_API_BASE + "/" + userId + "/threads_publish"))
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
            String postUrl = "https://www.threads.net/post/" + postId;

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
        try {
            checkRateLimit();
            String url = GRAPH_API_BASE + "/" + postId + "?access_token=" + encode(accessToken);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .DELETE()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "deletePost");
            recordApiCall();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("deletePost", e);
        } catch (RuntimeException e) {
            throw wrapException("deletePost", e);
        }
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
    public Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String url = GRAPH_API_BASE + "/" + platformPostId
                + "?fields=likes,replies,reposts,views"
                + "&access_token=" + encode(accessToken);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getPostStats");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return Optional.of(new SnsPostStats(
                platformPostId,
                SnsPlatform.THREADS,
                json.path("likes").asInt(0),
                json.path("replies").asInt(0),
                json.path("reposts").asInt(0),
                json.path("views").asInt(0),
                Instant.now()
            ));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("getPostStats", e);
        } catch (RuntimeException e) {
            throw wrapException("getPostStats", e);
        } finally {
            perfLog("ThreadsAdapter.getPostStats", perfStart);
        }
    }

    @Override
    public Optional<AccountStats> getAccountStats(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String url = GRAPH_API_BASE + "/me"
                + "?fields=id,username,followers_count"
                + "&access_token=" + encode(accessToken);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getAccountStats");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return Optional.of(new AccountStats(
                SnsPlatform.THREADS,
                json.path("username").asText(),
                json.path("followers_count").asInt(0),
                0,
                0,
                Instant.now()
            ));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("getAccountStats", e);
        } catch (RuntimeException e) {
            throw wrapException("getAccountStats", e);
        } finally {
            perfLog("ThreadsAdapter.getAccountStats", perfStart);
        }
    }

    @Override
    public void close() {
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
