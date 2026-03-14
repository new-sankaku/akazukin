package com.akazukin.adapter.mixi2;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.mixi2.Mixi2Client;
import com.akazukin.sdk.mixi2.Mixi2Config;
import com.akazukin.sdk.mixi2.model.Mixi2PostResponse;
import com.akazukin.sdk.mixi2.model.Mixi2TokenResponse;
import com.akazukin.sdk.mixi2.model.Mixi2User;

import java.time.Instant;
import java.util.Objects;

public class Mixi2Adapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final String POST_URL_PREFIX = "https://mixi.social/posts/";
    private static final int MIXI2_MAX_LENGTH = 1000;

    private final Mixi2Client client;

    public Mixi2Adapter(Mixi2Client client) {
        this.client = Objects.requireNonNull(client, "Mixi2Client must not be null");
    }

    public Mixi2Adapter(Mixi2Config config) {
        this(Mixi2Client.builder().config(config).build());
    }

    public Mixi2Adapter() {
        this(new Mixi2Config(
            System.getProperty("akazukin.mixi2.client-id", System.getenv("MIXI2_CLIENT_ID")),
            System.getProperty("akazukin.mixi2.client-secret", System.getenv("MIXI2_CLIENT_SECRET")),
            System.getProperty("akazukin.mixi2.redirect-uri", System.getenv("MIXI2_REDIRECT_URI"))
        ));
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.MIXI2;
    }

    @Override
    public int getMaxContentLength() {
        return MIXI2_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return client.getAuthorizationUrl(state);
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            Mixi2TokenResponse response = client.exchangeToken(code, callbackUrl);
            recordApiCall();
            return toSnsAuthToken(response);
        } catch (RuntimeException e) {
            throw wrapException("exchangeToken", e);
        }
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        try {
            checkRateLimit();
            Mixi2TokenResponse response = client.refreshToken(refreshToken);
            recordApiCall();
            return toSnsAuthToken(response);
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
            Mixi2User user = client.getProfile(accessToken);
            recordApiCall();
            SnsProfile profile = new SnsProfile(
                user.id(),
                user.displayName(),
                user.profileImageUrl(),
                0
            );
            cacheProfile(accessToken, profile);
            return profile;
        } catch (RuntimeException e) {
            throw wrapException("getProfile", e);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        try {
            checkRateLimit();
            Mixi2PostResponse response = client.post(accessToken, request.content());
            recordApiCall();
            return new PostResult(
                response.id(),
                POST_URL_PREFIX + response.id(),
                Instant.now()
            );
        } catch (RuntimeException e) {
            throw wrapException("post", e);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        try {
            checkRateLimit();
            client.deletePost(accessToken, postId);
            recordApiCall();
        } catch (RuntimeException e) {
            throw wrapException("deletePost", e);
        }
    }

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }

    private SnsAuthToken toSnsAuthToken(Mixi2TokenResponse response) {
        Instant expiresAt = response.expiresIn() > 0
            ? Instant.now().plusSeconds(response.expiresIn())
            : null;
        return new SnsAuthToken(
            response.accessToken(),
            response.refreshToken(),
            expiresAt,
            null
        );
    }
}
