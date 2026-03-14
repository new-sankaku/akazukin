package com.akazukin.adapter.tiktok;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.tiktok.TikTokClient;
import com.akazukin.sdk.tiktok.TikTokConfig;
import com.akazukin.sdk.tiktok.model.PublishResponse;
import com.akazukin.sdk.tiktok.model.TikTokUser;
import com.akazukin.sdk.tiktok.model.TokenResponse;

import java.time.Instant;
import java.util.Objects;

public class TikTokAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final int TIKTOK_MAX_LENGTH = 2200;
    private static final String DEFAULT_PRIVACY_LEVEL = "SELF_ONLY";

    private final TikTokClient client;

    public TikTokAdapter(TikTokClient client) {
        this.client = Objects.requireNonNull(client, "TikTokClient must not be null");
    }

    public TikTokAdapter(TikTokConfig config) {
        this(TikTokClient.builder().config(config).build());
    }

    public TikTokAdapter() {
        this(new TikTokConfig(
            System.getProperty("akazukin.tiktok.client-key", System.getenv("TIKTOK_CLIENT_KEY")),
            System.getProperty("akazukin.tiktok.client-secret", System.getenv("TIKTOK_CLIENT_SECRET")),
            System.getProperty("akazukin.tiktok.redirect-uri", System.getenv("TIKTOK_REDIRECT_URI"))
        ));
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.TIKTOK;
    }

    @Override
    public int getMaxContentLength() {
        return TIKTOK_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return client.getAuthorizationUrl(state);
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            TokenResponse response = client.exchangeToken(code);
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
            TokenResponse response = client.refreshToken(refreshToken);
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
            TikTokUser user = client.getMe(accessToken);
            recordApiCall();
            SnsProfile profile = new SnsProfile(
                user.username() != null ? user.username() : user.openId(),
                user.displayName(),
                user.avatarUrl(),
                user.followerCount()
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
            String title = request.content();
            if (request.mediaUrls() != null && !request.mediaUrls().isEmpty()) {
                String videoUrl = request.mediaUrls().get(0);
                PublishResponse response = client.initVideoPostByUrl(
                    accessToken, videoUrl, title, DEFAULT_PRIVACY_LEVEL);
                recordApiCall();
                return new PostResult(
                    response.publishId(),
                    null,
                    Instant.now()
                );
            }
            PublishResponse response = client.initPhotoPost(
                accessToken, title, DEFAULT_PRIVACY_LEVEL);
            recordApiCall();
            return new PostResult(
                response.publishId(),
                null,
                Instant.now()
            );
        } catch (RuntimeException e) {
            throw wrapException("post", e);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        throw new UnsupportedOperationException("TikTok API does not support deleting posts");
    }

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }

    private SnsAuthToken toSnsAuthToken(TokenResponse response) {
        Instant expiresAt = response.expiresIn() > 0
            ? Instant.now().plusSeconds(response.expiresIn())
            : null;
        return new SnsAuthToken(
            response.accessToken(),
            response.refreshToken(),
            expiresAt,
            response.scope()
        );
    }
}
