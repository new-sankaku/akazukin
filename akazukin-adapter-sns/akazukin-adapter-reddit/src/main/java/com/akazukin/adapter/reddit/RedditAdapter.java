package com.akazukin.adapter.reddit;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.reddit.RedditClient;
import com.akazukin.sdk.reddit.RedditConfig;

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
import java.util.Base64;
import java.util.Objects;

public class RedditAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String AUTH_URL = "https://www.reddit.com/api/v1/authorize";
    private static final String TOKEN_URL = "https://www.reddit.com/api/v1/access_token";
    private static final String API_BASE = "https://oauth.reddit.com";

    private final String clientId;
    private final String clientSecret;
    private final String userAgent;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RedditAdapter(String clientId, String clientSecret, String userAgent,
                         HttpClient httpClient, ObjectMapper objectMapper) {
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public RedditAdapter(String clientId, String clientSecret, String userAgent) {
        this(clientId, clientSecret, userAgent, SHARED_HTTP_CLIENT, SHARED_OBJECT_MAPPER);
    }

    public RedditAdapter() {
        this(
            System.getProperty("akazukin.reddit.client-id",
                System.getenv().getOrDefault("REDDIT_CLIENT_ID", "")),
            System.getProperty("akazukin.reddit.client-secret",
                System.getenv().getOrDefault("REDDIT_CLIENT_SECRET", "")),
            System.getProperty("akazukin.reddit.user-agent",
                System.getenv().getOrDefault("REDDIT_USER_AGENT", "akazukin/1.0"))
        );
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.REDDIT;
    }

    @Override
    public int getMaxContentLength() {
        return REDDIT_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return AUTH_URL
            + "?client_id=" + encode(clientId)
            + "&response_type=code"
            + "&state=" + encode(state)
            + "&redirect_uri=" + encode(callbackUrl)
            + "&duration=permanent"
            + "&scope=" + encode("identity submit read");
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String body = "grant_type=authorization_code"
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(callbackUrl);

            HttpRequest request = buildTokenRequest(body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "exchangeToken");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return toSnsAuthToken(json);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("exchangeToken", e);
        } catch (RuntimeException e) {
            throw wrapException("exchangeToken", e);
        } finally {
            perfLog("RedditAdapter.exchangeToken", perfStart);
        }
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String body = "grant_type=refresh_token"
                + "&refresh_token=" + encode(refreshToken);

            HttpRequest request = buildTokenRequest(body);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "refreshToken");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return toSnsAuthToken(json);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("refreshToken", e);
        } catch (RuntimeException e) {
            throw wrapException("refreshToken", e);
        } finally {
            perfLog("RedditAdapter.refreshToken", perfStart);
        }
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            SnsProfile cached = getCachedProfile(accessToken);
            if (cached != null) {
                return cached;
            }
            checkRateLimit();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/api/v1/me"))
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getProfile");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            int karma = json.path("link_karma").asInt(0) + json.path("comment_karma").asInt(0);

            SnsProfile profile = new SnsProfile(
                json.path("name").asText(),
                json.path("name").asText(""),
                json.path("icon_img").asText(null),
                karma
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
        } finally {
            perfLog("RedditAdapter.getProfile", perfStart);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String content = request.content();
            String subreddit;
            String title;
            String bodyText;

            int firstSep = content.indexOf(':');
            int secondSep = content.indexOf(':', firstSep + 1);
            if (firstSep > 0 && secondSep > firstSep) {
                subreddit = content.substring(0, firstSep).trim();
                title = content.substring(firstSep + 1, secondSep).trim();
                bodyText = content.substring(secondSep + 1).trim();
            } else {
                throw new IllegalArgumentException(
                    "Reddit post content must be in format 'subreddit:title:body'. "
                        + "Example: 'programming:My Post:Post body text'"
                );
            }

            String postBody = "sr=" + encode(subreddit)
                + "&kind=self"
                + "&title=" + encode(title)
                + "&text=" + encode(bodyText)
                + "&api_type=json";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/api/submit"))
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postBody))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "post");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            JsonNode data = json.path("json").path("data");
            String postId = data.path("name").asText();
            String postUrl = data.path("url").asText("https://www.reddit.com/r/" + subreddit);

            return new PostResult(postId, postUrl, Instant.now());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("post", e);
        } catch (RuntimeException e) {
            throw wrapException("post", e);
        } finally {
            perfLog("RedditAdapter.post", perfStart);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String body = "id=" + encode(postId);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/api/del"))
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", userAgent)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
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
        } finally {
            perfLog("RedditAdapter.deletePost", perfStart);
        }
    }

    private HttpRequest buildTokenRequest(String body) {
        String credentials = clientId + ":" + clientSecret;
        String basicAuth = Base64.getEncoder().encodeToString(
            credentials.getBytes(StandardCharsets.UTF_8));

        return HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Authorization", "Basic " + basicAuth)
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(READ_TIMEOUT)
            .build();
    }

    private SnsAuthToken toSnsAuthToken(JsonNode json) {
        int expiresIn = json.path("expires_in").asInt(0);
        Instant expiresAt = expiresIn > 0 ? Instant.now().plusSeconds(expiresIn) : null;

        return new SnsAuthToken(
            json.path("access_token").asText(),
            json.path("refresh_token").asText(null),
            expiresAt,
            json.path("scope").asText(null)
        );
    }

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
