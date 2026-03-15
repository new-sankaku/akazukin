package com.akazukin.adapter.mastodon;

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

public class MastodonAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String instanceUrl;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MastodonAdapter(String instanceUrl, String clientId, String clientSecret,
                           HttpClient httpClient, ObjectMapper objectMapper) {
        this.instanceUrl = Objects.requireNonNull(instanceUrl, "instanceUrl must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public MastodonAdapter(String instanceUrl, String clientId, String clientSecret) {
        this(instanceUrl, clientId, clientSecret, SHARED_HTTP_CLIENT, SHARED_OBJECT_MAPPER);
    }

    public MastodonAdapter() {
        this(
            System.getProperty("akazukin.mastodon.instance-url",
                System.getenv().getOrDefault("MASTODON_INSTANCE_URL", "https://mastodon.social")),
            System.getProperty("akazukin.mastodon.client-id",
                System.getenv().getOrDefault("MASTODON_CLIENT_ID", "")),
            System.getProperty("akazukin.mastodon.client-secret",
                System.getenv().getOrDefault("MASTODON_CLIENT_SECRET", ""))
        );
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.MASTODON;
    }

    @Override
    public int getMaxContentLength() {
        return MASTODON_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return instanceUrl + "/oauth/authorize"
            + "?client_id=" + encode(clientId)
            + "&scope=" + encode("read write")
            + "&redirect_uri=" + encode(callbackUrl)
            + "&response_type=code"
            + "&state=" + encode(state);
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            String body = "grant_type=authorization_code"
                + "&code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&redirect_uri=" + encode(callbackUrl)
                + "&scope=" + encode("read write");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(instanceUrl + "/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
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
                json.path("refresh_token").asText(null),
                expiresAt,
                json.path("scope").asText(null)
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
            String body = "grant_type=refresh_token"
                + "&refresh_token=" + encode(refreshToken)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(instanceUrl + "/oauth/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
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
                json.path("refresh_token").asText(null),
                expiresAt,
                json.path("scope").asText(null)
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
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(instanceUrl + "/api/v1/accounts/verify_credentials"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getProfile");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            SnsProfile profile = new SnsProfile(
                json.path("acct").asText(),
                json.path("display_name").asText(""),
                json.path("avatar").asText(null),
                json.path("followers_count").asInt(0)
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
            String body = "status=" + encode(request.content());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(instanceUrl + "/api/v1/statuses"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "post");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            String id = json.path("id").asText();
            String url = json.path("url").asText(instanceUrl + "/@unknown/" + id);
            String createdAt = json.path("created_at").asText(null);
            Instant publishedAt = createdAt != null ? Instant.parse(createdAt) : Instant.now();

            return new PostResult(id, url, publishedAt);
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
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(instanceUrl + "/api/v1/statuses/" + postId))
                .header("Authorization", "Bearer " + accessToken)
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

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
