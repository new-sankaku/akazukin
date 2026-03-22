package com.akazukin.adapter.bluesky;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.bluesky.BlueskyClient;
import com.akazukin.sdk.bluesky.BlueskyConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BlueskyAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECTION_TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_2)
        .build();

    private static final ObjectMapper SHARED_OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final Logger LOG = Logger.getLogger(BlueskyAdapter.class.getName());

    private static final String BSKY_SETTINGS_URL = "https://bsky.app/settings/app-passwords";
    private static final String DEFAULT_SERVICE_URL = "https://bsky.social";

    private final String serviceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public BlueskyAdapter(String serviceUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.serviceUrl = Objects.requireNonNull(serviceUrl, "serviceUrl must not be null");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public BlueskyAdapter(String serviceUrl) {
        this(serviceUrl, SHARED_HTTP_CLIENT, SHARED_OBJECT_MAPPER);
    }

    public BlueskyAdapter() {
        this(System.getProperty("akazukin.bluesky.service-url", DEFAULT_SERVICE_URL));
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.BLUESKY;
    }

    @Override
    public int getMaxContentLength() {
        return BLUESKY_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return BSKY_SETTINGS_URL;
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String identifier = callbackUrl;
            String password = code;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("identifier", identifier);
            body.put("password", password);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/com.atproto.server.createSession"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "exchangeToken");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return new SnsAuthToken(
                json.path("accessJwt").asText(),
                json.path("refreshJwt").asText(),
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
        } finally {
            perfLog("BlueskyAdapter.exchangeToken", perfStart);
        }
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/com.atproto.server.refreshSession"))
                .header("Authorization", "Bearer " + refreshToken)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "refreshToken");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return new SnsAuthToken(
                json.path("accessJwt").asText(),
                json.path("refreshJwt").asText(),
                null,
                null
            );
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("refreshToken", e);
        } catch (RuntimeException e) {
            throw wrapException("refreshToken", e);
        } finally {
            perfLog("BlueskyAdapter.refreshToken", perfStart);
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
            String did = extractDidFromToken(accessToken);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/app.bsky.actor.getProfile?actor="
                    + URLEncoder.encode(did, StandardCharsets.UTF_8)))
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
                json.path("handle").asText(),
                json.path("displayName").asText(""),
                json.path("avatar").asText(null),
                json.path("followersCount").asInt(0)
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
            perfLog("BlueskyAdapter.getProfile", perfStart);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);

            ObjectNode record = objectMapper.createObjectNode();
            record.put("$type", "app.bsky.feed.post");
            record.put("text", request.content());
            record.put("createdAt", Instant.now().toString());

            ObjectNode body = objectMapper.createObjectNode();
            body.put("repo", did);
            body.put("collection", "app.bsky.feed.post");
            body.set("record", record);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/com.atproto.repo.createRecord"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "post");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            String uri = json.path("uri").asText();
            String rkey = extractRkey(uri);
            String handle = did;
            String postUrl = "https://bsky.app/profile/" + handle + "/post/" + rkey;

            return new PostResult(rkey, postUrl, Instant.now());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("post", e);
        } catch (RuntimeException e) {
            throw wrapException("post", e);
        } finally {
            perfLog("BlueskyAdapter.post", perfStart);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("repo", did);
            body.put("collection", "app.bsky.feed.post");
            body.put("rkey", postId);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/com.atproto.repo.deleteRecord"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
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
            perfLog("BlueskyAdapter.deletePost", perfStart);
        }
    }

    @Override
    public void reply(String accessToken, String postId, String content) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);

            String postUri = "at://" + did + "/app.bsky.feed.post/" + postId;

            ObjectNode parentRef = objectMapper.createObjectNode();
            parentRef.put("uri", postUri);
            parentRef.put("cid", postId);

            ObjectNode replyRef = objectMapper.createObjectNode();
            replyRef.set("root", parentRef);
            replyRef.set("parent", parentRef);

            ObjectNode record = objectMapper.createObjectNode();
            record.put("$type", "app.bsky.feed.post");
            record.put("text", content);
            record.put("createdAt", Instant.now().toString());
            record.set("reply", replyRef);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("repo", did);
            body.put("collection", "app.bsky.feed.post");
            body.set("record", record);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/com.atproto.repo.createRecord"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "reply");
            recordApiCall();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("reply", e);
        } catch (RuntimeException e) {
            throw wrapException("reply", e);
        } finally {
            perfLog("BlueskyAdapter.reply", perfStart);
        }
    }

    @Override
    public void mention(String accessToken, String userId, String content) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);
            String mentionText = "@" + userId + " " + content;

            ObjectNode facet = objectMapper.createObjectNode();
            ObjectNode index = objectMapper.createObjectNode();
            index.put("byteStart", 0);
            index.put("byteEnd", ("@" + userId).getBytes(StandardCharsets.UTF_8).length);
            facet.set("index", index);

            ObjectNode feature = objectMapper.createObjectNode();
            feature.put("$type", "app.bsky.richtext.facet#mention");
            feature.put("did", userId);
            facet.set("features", objectMapper.createArrayNode().add(feature));

            ObjectNode record = objectMapper.createObjectNode();
            record.put("$type", "app.bsky.feed.post");
            record.put("text", mentionText);
            record.put("createdAt", Instant.now().toString());
            record.set("facets", objectMapper.createArrayNode().add(facet));

            ObjectNode body = objectMapper.createObjectNode();
            body.put("repo", did);
            body.put("collection", "app.bsky.feed.post");
            body.set("record", record);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/com.atproto.repo.createRecord"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "mention");
            recordApiCall();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("mention", e);
        } catch (RuntimeException e) {
            throw wrapException("mention", e);
        } finally {
            perfLog("BlueskyAdapter.mention", perfStart);
        }
    }

    @Override
    public List<SnsProfile> getFollowers(String accessToken, int limit) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);
            int maxResults = Math.max(1, Math.min(limit, 100));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/app.bsky.graph.getFollowers?actor="
                    + URLEncoder.encode(did, StandardCharsets.UTF_8) + "&limit=" + maxResults))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getFollowers");
            recordApiCall();
            return parseGraphProfiles(response, "followers");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("getFollowers", e);
        } catch (RuntimeException e) {
            throw wrapException("getFollowers", e);
        } finally {
            perfLog("BlueskyAdapter.getFollowers", perfStart);
        }
    }

    @Override
    public List<SnsProfile> getFollowing(String accessToken, int limit) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);
            int maxResults = Math.max(1, Math.min(limit, 100));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/app.bsky.graph.getFollows?actor="
                    + URLEncoder.encode(did, StandardCharsets.UTF_8) + "&limit=" + maxResults))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getFollowing");
            recordApiCall();
            return parseGraphProfiles(response, "follows");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw wrapException("getFollowing", e);
        } catch (RuntimeException e) {
            throw wrapException("getFollowing", e);
        } finally {
            perfLog("BlueskyAdapter.getFollowing", perfStart);
        }
    }

    private List<SnsProfile> parseGraphProfiles(HttpResponse<String> response, String arrayField)
            throws IOException {
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.get(arrayField);
        if (data == null || !data.isArray()) {
            return List.of();
        }
        List<SnsProfile> profiles = new ArrayList<>();
        for (JsonNode node : data) {
            profiles.add(new SnsProfile(
                node.path("handle").asText(),
                node.path("displayName").asText(""),
                node.path("avatar").asText(null),
                node.path("followersCount").asInt(0)
            ));
        }
        return profiles;
    }

    private String extractRkey(String atUri) {
        int lastSlash = atUri.lastIndexOf('/');
        if (lastSlash < 0) {
            return atUri;
        }
        return atUri.substring(lastSlash + 1);
    }

    @Override
    public Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);
            String postUri = "at://" + did + "/app.bsky.feed.post/" + platformPostId;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/app.bsky.feed.getPostThread?uri="
                    + URLEncoder.encode(postUri, StandardCharsets.UTF_8) + "&depth=0"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            checkResponseStatus(response, "getPostStats");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            JsonNode post = json.path("thread").path("post");
            return Optional.of(new SnsPostStats(
                platformPostId,
                SnsPlatform.BLUESKY,
                post.path("likeCount").asInt(0),
                post.path("replyCount").asInt(0),
                post.path("repostCount").asInt(0),
                0,
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
            perfLog("BlueskyAdapter.getPostStats", perfStart);
        }
    }

    @Override
    public Optional<AccountStats> getAccountStats(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String did = extractDidFromToken(accessToken);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/xrpc/app.bsky.actor.getProfile?actor="
                    + URLEncoder.encode(did, StandardCharsets.UTF_8)))
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
                SnsPlatform.BLUESKY,
                json.path("handle").asText(),
                json.path("followersCount").asInt(0),
                json.path("followsCount").asInt(0),
                json.path("postsCount").asInt(0),
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
            perfLog("BlueskyAdapter.getAccountStats", perfStart);
        }
    }

    @Override
    public void close() {
    }

    private String extractDidFromToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(
                    java.util.Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
                );
                JsonNode json = objectMapper.readTree(payload);
                String sub = json.path("sub").asText(null);
                if (sub != null && !sub.isEmpty()) {
                    return sub;
                }
            }
        } catch (IllegalArgumentException | IOException e) {
            LOG.log(Level.WARNING,
                "Failed to decode DID from Bluesky JWT token, using token as-is: " + e.getMessage(), e);
        }
        return accessToken;
    }

}
