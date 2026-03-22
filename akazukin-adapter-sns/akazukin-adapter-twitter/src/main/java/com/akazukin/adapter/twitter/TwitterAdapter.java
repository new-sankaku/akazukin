package com.akazukin.adapter.twitter;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.twitter.TwitterClient;
import com.akazukin.sdk.twitter.TwitterConfig;
import com.akazukin.sdk.twitter.auth.OAuth2PkceFlow;
import com.akazukin.sdk.twitter.model.TokenResponse;
import com.akazukin.sdk.twitter.model.TweetMetrics;
import com.akazukin.sdk.twitter.model.TweetResponse;
import com.akazukin.sdk.twitter.model.TwitterUser;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
            envOrDefault("akazukin.twitter.client-id", "TWITTER_CLIENT_ID", "unconfigured"),
            envOrDefault("akazukin.twitter.client-secret", "TWITTER_CLIENT_SECRET", "unconfigured"),
            System.getProperty("akazukin.twitter.redirect-uri", System.getenv("TWITTER_REDIRECT_URI"))
        ));
    }

    private static String envOrDefault(String sysProp, String envVar, String defaultValue) {
        String value = System.getProperty(sysProp);
        if (value != null && !value.isBlank()) return value;
        value = System.getenv(envVar);
        if (value != null && !value.isBlank()) return value;
        return defaultValue;
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
        long perfStart = System.nanoTime();
        try {
            cleanupExpiredVerifiers();
            String codeVerifier = OAuth2PkceFlow.generateCodeVerifier();
            String codeChallenge = OAuth2PkceFlow.generateCodeChallenge(codeVerifier);
            codeVerifiers.put(callbackUrl,
                new AbstractMap.SimpleImmutableEntry<>(codeVerifier, Instant.now()));
            return client.getAuthorizationUrl(state, codeChallenge);
        } finally {
            perfLog("TwitterAdapter.getAuthorizationUrl", perfStart);
        }
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        long perfStart = System.nanoTime();
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
        } finally {
            perfLog("TwitterAdapter.exchangeToken", perfStart);
        }
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            TokenResponse response = client.refreshToken(refreshToken);
            recordApiCall();
            return toSnsAuthToken(response);
        } catch (RuntimeException e) {
            throw wrapException("refreshToken", e);
        } finally {
            perfLog("TwitterAdapter.refreshToken", perfStart);
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
            TwitterUser user = client.getMe(accessToken);
            recordApiCall();
            SnsProfile profile = new SnsProfile(
                user.username(),
                user.name(),
                user.profileImageUrl(),
                user.followersCount()
            );
            cacheProfile(accessToken, profile);
            return profile;
        } catch (RuntimeException e) {
            throw wrapException("getProfile", e);
        } finally {
            perfLog("TwitterAdapter.getProfile", perfStart);
        }
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        long perfStart = System.nanoTime();
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
        } finally {
            perfLog("TwitterAdapter.post", perfStart);
        }
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            client.deleteTweet(accessToken, postId);
            recordApiCall();
        } catch (RuntimeException e) {
            throw wrapException("deletePost", e);
        } finally {
            perfLog("TwitterAdapter.deletePost", perfStart);
        }
    }

    @Override
    public void reply(String accessToken, String postId, String content) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            client.replyToTweet(accessToken, postId, content);
            recordApiCall();
        } catch (RuntimeException e) {
            throw wrapException("reply", e);
        } finally {
            perfLog("TwitterAdapter.reply", perfStart);
        }
    }

    @Override
    public void mention(String accessToken, String userId, String content) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            String mentionText = "@" + userId + " " + content;
            client.postTweet(accessToken, mentionText);
            recordApiCall();
        } catch (RuntimeException e) {
            throw wrapException("mention", e);
        } finally {
            perfLog("TwitterAdapter.mention", perfStart);
        }
    }

    @Override
    public List<SnsProfile> getFollowers(String accessToken, int limit) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            TwitterUser me = client.getMe(accessToken);
            List<TwitterUser> followers = client.getFollowers(accessToken, me.id(), limit);
            recordApiCall();
            return followers.stream()
                .map(u -> new SnsProfile(u.username(), u.name(), u.profileImageUrl(), u.followersCount()))
                .toList();
        } catch (RuntimeException e) {
            throw wrapException("getFollowers", e);
        } finally {
            perfLog("TwitterAdapter.getFollowers", perfStart);
        }
    }

    @Override
    public List<SnsProfile> getFollowing(String accessToken, int limit) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            TwitterUser me = client.getMe(accessToken);
            List<TwitterUser> following = client.getFollowing(accessToken, me.id(), limit);
            recordApiCall();
            return following.stream()
                .map(u -> new SnsProfile(u.username(), u.name(), u.profileImageUrl(), u.followersCount()))
                .toList();
        } catch (RuntimeException e) {
            throw wrapException("getFollowing", e);
        } finally {
            perfLog("TwitterAdapter.getFollowing", perfStart);
        }
    }

    @Override
    public Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            TweetMetrics metrics = client.getTweetMetrics(accessToken, platformPostId);
            recordApiCall();
            if (metrics == null) {
                return Optional.empty();
            }
            return Optional.of(new SnsPostStats(
                platformPostId,
                SnsPlatform.TWITTER,
                metrics.likeCount(),
                metrics.replyCount(),
                metrics.retweetCount(),
                metrics.impressionCount(),
                Instant.now()
            ));
        } catch (RuntimeException e) {
            throw wrapException("getPostStats", e);
        } finally {
            perfLog("TwitterAdapter.getPostStats", perfStart);
        }
    }

    @Override
    public Optional<AccountStats> getAccountStats(String accessToken) {
        long perfStart = System.nanoTime();
        try {
            checkRateLimit();
            TwitterUser user = client.getMe(accessToken);
            recordApiCall();
            return Optional.of(new AccountStats(
                SnsPlatform.TWITTER,
                user.username(),
                user.followersCount(),
                user.followingCount(),
                0,
                Instant.now()
            ));
        } catch (RuntimeException e) {
            throw wrapException("getAccountStats", e);
        } finally {
            perfLog("TwitterAdapter.getAccountStats", perfStart);
        }
    }

    @Override
    public void close() {
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
