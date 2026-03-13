package com.akazukin.adapter.twitter;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.twitter.TwitterClient;
import com.akazukin.sdk.twitter.TwitterConfig;
import com.akazukin.sdk.twitter.auth.OAuth2PkceFlow;
import com.akazukin.sdk.twitter.model.TokenResponse;
import com.akazukin.sdk.twitter.model.TweetResponse;
import com.akazukin.sdk.twitter.model.TwitterUser;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TwitterAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final String TWEET_URL_PREFIX = "https://twitter.com/i/status/";
    private static final long CODE_VERIFIER_TTL_MILLIS = 10 * 60 * 1000L;

    private final TwitterClient client;
    private final ConcurrentHashMap<String, Map.Entry<String, Instant>> codeVerifiers =
            new ConcurrentHashMap<>();

    public TwitterAdapter(TwitterClient client) {
        this.client = Objects.requireNonNull(client, "TwitterClient must not be null");
    }

    public TwitterAdapter(TwitterConfig config) {
        this(TwitterClient.builder().config(config).build());
    }

    public TwitterAdapter() {
        this(new TwitterConfig(
            System.getProperty("akazukin.twitter.client-id", System.getenv("TWITTER_CLIENT_ID")),
            System.getProperty("akazukin.twitter.client-secret", System.getenv("TWITTER_CLIENT_SECRET")),
            System.getProperty("akazukin.twitter.redirect-uri", System.getenv("TWITTER_REDIRECT_URI"))
        ));
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.TWITTER;
    }

    @Override
    public int getMaxContentLength() {
        return TWITTER_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        try {
            checkRateLimit();
            cleanupExpiredVerifiers();
            String codeVerifier = OAuth2PkceFlow.generateCodeVerifier();
            String codeChallenge = OAuth2PkceFlow.generateCodeChallenge(codeVerifier);
            codeVerifiers.put(callbackUrl,
                new AbstractMap.SimpleImmutableEntry<>(codeVerifier, Instant.now()));
            String url = client.getAuthorizationUrl(state, codeChallenge);
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
            cleanupExpiredVerifiers();
            Map.Entry<String, Instant> entry = codeVerifiers.remove(callbackUrl);
            if (entry == null) {
                throw new IllegalStateException(
                    "No code verifier found. getAuthorizationUrl must be called first."
                );
            }
            String codeVerifier = entry.getKey();
            TokenResponse response = client.exchangeToken(code, codeVerifier);
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
        try {
            checkRateLimit();
            TwitterUser user = client.getMe(accessToken);
            recordApiCall();
            return new SnsProfile(
                user.username(),
                user.name(),
                user.profileImageUrl(),
                user.followersCount()
            );
        } catch (RuntimeException e) {
            throw wrapException("getProfile", e);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        try {
            checkRateLimit();
            TweetResponse response = client.postTweet(accessToken, request.content());
            recordApiCall();
            return new PostResult(
                response.id(),
                TWEET_URL_PREFIX + response.id(),
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
            client.deleteTweet(accessToken, postId);
            recordApiCall();
        } catch (RuntimeException e) {
            throw wrapException("deletePost", e);
        }
    }

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }

    private void cleanupExpiredVerifiers() {
        Instant cutoff = Instant.now().minusMillis(CODE_VERIFIER_TTL_MILLIS);
        Iterator<Map.Entry<String, Map.Entry<String, Instant>>> it =
                codeVerifiers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Map.Entry<String, Instant>> entry = it.next();
            if (entry.getValue().getValue().isBefore(cutoff)) {
                it.remove();
            }
        }
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
