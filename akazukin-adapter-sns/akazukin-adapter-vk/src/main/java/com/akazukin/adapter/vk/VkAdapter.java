package com.akazukin.adapter.vk;

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
import java.time.Instant;
import java.util.Objects;

public class VkAdapter extends AbstractSnsAdapter {

    private static final String AUTH_URL = "https://oauth.vk.com/authorize";
    private static final String TOKEN_URL = "https://oauth.vk.com/access_token";
    private static final String API_BASE = "https://api.vk.com/method";
    private static final String API_VERSION = "5.199";

    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VkAdapter(String clientId, String clientSecret,
                     HttpClient httpClient, ObjectMapper objectMapper) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public VkAdapter(String clientId, String clientSecret) {
        this(
            clientId,
            clientSecret,
            HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
            new ObjectMapper()
        );
    }

    public VkAdapter() {
        this(
            System.getProperty("akazukin.vk.client-id",
                System.getenv().getOrDefault("VK_CLIENT_ID", "")),
            System.getProperty("akazukin.vk.client-secret",
                System.getenv().getOrDefault("VK_CLIENT_SECRET", ""))
        );
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.VK;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        try {
            checkRateLimit();
            String url = AUTH_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(callbackUrl)
                + "&display=page"
                + "&scope=" + encode("wall,offline")
                + "&response_type=code"
                + "&state=" + encode(state)
                + "&v=" + API_VERSION;
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
            checkVkError(json, "exchangeToken");
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
            recordApiCall();
            return new SnsAuthToken(refreshToken, null, null, null);
        } catch (RuntimeException e) {
            throw wrapException("refreshToken", e);
        }
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        try {
            checkRateLimit();
            String body = "access_token=" + encode(accessToken)
                + "&fields=" + encode("photo_200,counters")
                + "&v=" + API_VERSION;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/users.get"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getProfile");
            JsonNode json = objectMapper.readTree(response.body());
            checkVkError(json, "getProfile");
            recordApiCall();

            JsonNode users = json.path("response");
            if (!users.isArray() || users.isEmpty()) {
                throw new RuntimeException("Empty response from users.get");
            }
            JsonNode user = users.get(0);

            String firstName = user.path("first_name").asText("");
            String lastName = user.path("last_name").asText("");
            String displayName = (firstName + " " + lastName).trim();
            int followersCount = user.path("counters").path("followers").asInt(0);

            return new SnsProfile(
                String.valueOf(user.path("id").asLong()),
                displayName,
                user.path("photo_200").asText(null),
                followersCount
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
            String body = "access_token=" + encode(accessToken)
                + "&message=" + encode(request.content())
                + "&v=" + API_VERSION;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/wall.post"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "post");
            JsonNode json = objectMapper.readTree(response.body());
            checkVkError(json, "post");
            recordApiCall();

            int postId = json.path("response").path("post_id").asInt();
            String postUrl = "https://vk.com/wall" + getUserId(accessToken) + "_" + postId;

            return new PostResult(
                String.valueOf(postId),
                postUrl,
                Instant.now()
            );
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
            String body = "access_token=" + encode(accessToken)
                + "&post_id=" + encode(postId)
                + "&v=" + API_VERSION;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/wall.delete"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "deletePost");
            JsonNode json = objectMapper.readTree(response.body());
            checkVkError(json, "deletePost");
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
        String body = "access_token=" + encode(accessToken) + "&v=" + API_VERSION;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE + "/users.get"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkResponseStatus(response, "getUserId");
        JsonNode json = objectMapper.readTree(response.body());
        JsonNode users = json.path("response");
        if (users.isArray() && !users.isEmpty()) {
            return String.valueOf(users.get(0).path("id").asLong());
        }
        return "0";
    }

    private void checkVkError(JsonNode json, String operation) {
        JsonNode error = json.path("error");
        if (!error.isMissingNode()) {
            int errorCode = error.path("error_code").asInt(0);
            String errorMsg = error.path("error_msg").asText("Unknown VK error");
            throw wrapException(operation,
                new RuntimeException("VK API error " + errorCode + ": " + errorMsg));
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
