package com.akazukin.adapter.bluesky;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.bluesky.BlueskyClient;
import com.akazukin.sdk.bluesky.BlueskyConfig;

import java.io.IOException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BlueskyAdapter extends AbstractSnsAdapter {

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
        this(
            serviceUrl,
            HttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
            new ObjectMapper()
        );
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
        }
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
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
        }
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
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
            checkResponseStatus(response, "getProfile");
            JsonNode json = objectMapper.readTree(response.body());
            recordApiCall();

            return new SnsProfile(
                json.path("handle").asText(),
                json.path("displayName").asText(""),
                json.path("avatar").asText(null),
                json.path("followersCount").asInt(0)
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
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
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
        }
    }

    private String extractRkey(String atUri) {
        int lastSlash = atUri.lastIndexOf('/');
        if (lastSlash < 0) {
            return atUri;
        }
        return atUri.substring(lastSlash + 1);
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
