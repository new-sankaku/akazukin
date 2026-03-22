package com.akazukin.adapter.pinterest;

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
import com.fasterxml.jackson.databind.node.ObjectNode;

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
import java.util.Optional;

public class PinterestAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final int PINTEREST_MAX_LENGTH = 500;

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String AUTH_URL = "https://www.pinterest.com/oauth/";
    private static final String TOKEN_URL = "https://api.pinterest.com/v5/oauth/token";
    private static final String API_BASE = "https://api.pinterest.com/v5";

    private final String appId;
    private final String appSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PinterestAdapter(String appId, String appSecret,
                            HttpClient httpClient, ObjectMapper objectMapper) {
        this.appId = Objects.requireNonNull(appId, "appId must not be null");
        this.appSecret = Objects.requireNonNull(appSecret, "appSecret must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public PinterestAdapter(String appId, String appSecret) {
        this(appId, appSecret, SHARED_HTTP_CLIENT, SHARED_OBJECT_MAPPER);
    }

    public PinterestAdapter() {
        this(
            System.getProperty("akazukin.pinterest.app-id",
                System.getenv().getOrDefault("PINTEREST_APP_ID", "")),
            System.getProperty("akazukin.pinterest.app-secret",
                System.getenv().getOrDefault("PINTEREST_APP_SECRET", ""))
        );
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.PINTEREST;
    }

    @Override
    public int getMaxContentLength() {
        return PINTEREST_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return AUTH_URL
            + "?client_id=" + encode(appId)
            + "&redirect_uri=" + encode(callbackUrl)
            + "&response_type=code"
            + "&scope=" + encode("boards:read,pins:read,pins:write")
            + "&state=" + encode(state);
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
            perfLog("PinterestAdapter.exchangeToken", perfStart);
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
            perfLog("PinterestAdapter.refreshToken", perfStart);
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
                .uri(URI.create(API_BASE + "/user_account"))
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
                json.path("username").asText(),
                json.path("business_name").asText(json.path("username").asText("")),
                json.path("profile_image").asText(null),
                json.path("follower_count").asInt(0)
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
            perfLog("PinterestAdapter.getProfile", perfStart);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String content = request.content();
            String boardId;
            String title;
            String description;

            int firstSep = content.indexOf(':');
            int secondSep = content.indexOf(':', firstSep + 1);
            if (firstSep > 0 && secondSep > firstSep) {
                boardId = content.substring(0, firstSep).trim();
                title = content.substring(firstSep + 1, secondSep).trim();
                description = content.substring(secondSep + 1).trim();
            } else if (firstSep > 0) {
                boardId = content.substring(0, firstSep).trim();
                title = content.substring(firstSep + 1).trim();
                description = title;
            } else {
                throw new IllegalArgumentException(
                    "Pinterest post content must be in format 'boardId:title:description'. "
                        + "Example: 'my-board:Pin Title:Pin description text'"
                );
            }

            if (request.mediaUrls().isEmpty()) {
                throw new IllegalArgumentException(
                    "Pinterest requires at least one media URL. Pins must have an image."
                );
            }

            String imageUrl = request.mediaUrls().get(0);

            ObjectNode bodyNode = objectMapper.createObjectNode();
            bodyNode.put("board_id", boardId);
            bodyNode.put("title", title);
            bodyNode.put("description", description);
            ObjectNode mediaSource = objectMapper.createObjectNode();
            mediaSource.put("source_type", "image_url");
            mediaSource.put("url", imageUrl);
            bodyNode.set("media_source", mediaSource);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/pins"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(bodyNode)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "post");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            String pinId = json.path("id").asText();
            String pinUrl = json.path("link").asText("https://www.pinterest.com/pin/" + pinId);

            return new PostResult(pinId, pinUrl, Instant.now());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("post", e);
        } catch (RuntimeException e) {
            throw wrapException("post", e);
        } finally {
            perfLog("PinterestAdapter.post", perfStart);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/pins/" + postId))
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
        } finally {
            perfLog("PinterestAdapter.deletePost", perfStart);
        }
    }

    private HttpRequest buildTokenRequest(String body) {
        String credentials = appId + ":" + appSecret;
        String basicAuth = Base64.getEncoder().encodeToString(
            credentials.getBytes(StandardCharsets.UTF_8));

        return HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Authorization", "Basic " + basicAuth)
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
    public Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/pins/" + platformPostId
                    + "?pin_metrics=true"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getPostStats");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            JsonNode metrics = json.path("pin_metrics").path("all_time");
            return Optional.of(new SnsPostStats(
                platformPostId,
                SnsPlatform.PINTEREST,
                metrics.path("save").asInt(0),
                metrics.path("pin_click").asInt(0),
                0,
                metrics.path("impression").asInt(0),
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
            perfLog("PinterestAdapter.getPostStats", perfStart);
        }
    }

    @Override
    public Optional<AccountStats> getAccountStats(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/user_account"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getAccountStats");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return Optional.of(new AccountStats(
                SnsPlatform.PINTEREST,
                json.path("username").asText(),
                json.path("follower_count").asInt(0),
                json.path("following_count").asInt(0),
                json.path("pin_count").asInt(0),
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
            perfLog("PinterestAdapter.getAccountStats", perfStart);
        }
    }

    @Override
    public void close() {
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
