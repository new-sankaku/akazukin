package com.akazukin.adapter.threads;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

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
import java.time.Instant;
import java.util.Objects;

public class ThreadsAdapter extends AbstractSnsAdapter {

    private static final String GRAPH_API_BASE = "https://graph.threads.net/v1.0";
    private static final String AUTH_URL = "https://threads.net/oauth/authorize";
    private static final String TOKEN_URL = "https://graph.threads.net/oauth/access_token";

    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ThreadsAdapter(String clientId, String clientSecret,
                          HttpClient httpClient, ObjectMapper objectMapper) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public ThreadsAdapter(String clientId, String clientSecret) {
        this(
            clientId,
            clientSecret,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
            new ObjectMapper()
        );
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
    public String getAuthorizationUrl(String callbackUrl, String state) {
        try {
            checkRateLimit();
            String url = AUTH_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(callbackUrl)
                + "&scope=" + encode("threads_basic,threads_content_publish")
                + "&response_type=code"
                + "&state=" + encode(state);
            recordApiCall();
            return url;
        } catch (Exception e) {
            throw wrapException("getAuthorizationUrl", e);
        }
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
                .timeout(Duration.ofSeconds(10))
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
        } catch (Exception e) {
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
                .timeout(Duration.ofSeconds(10))
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
        } catch (Exception e) {
            throw wrapException("refreshToken", e);
        }
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        try {
            checkRateLimit();
            String url = GRAPH_API_BASE + "/me"
                + "?fields=id,username,name,threads_profile_picture_url"
                + "&access_token=" + encode(accessToken);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getProfile");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return new SnsProfile(
                json.path("username").asText(),
                json.path("name").asText(""),
                json.path("threads_profile_picture_url").asText(null),
                0
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("getProfile", e);
        } catch (Exception e) {
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
                .timeout(Duration.ofSeconds(10))
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
                .timeout(Duration.ofSeconds(10))
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
        } catch (Exception e) {
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
                .timeout(Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "deletePost");
            recordApiCall();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("deletePost", e);
        } catch (Exception e) {
            throw wrapException("deletePost", e);
        }
    }

    private String getUserId(String accessToken) throws IOException, InterruptedException {
        String url = GRAPH_API_BASE + "/me?fields=id&access_token=" + encode(accessToken);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponseStatus(response, "getUserId");
        JsonNode json = objectMapper.readTree(response.body());
        return json.path("id").asText();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void checkResponseStatus(HttpResponse<String> response, String operation) {
        if (response.statusCode() >= 400) {
            throw wrapException(operation,
                new RuntimeException("HTTP " + response.statusCode() + ": " + response.body()));
        }
    }
}
