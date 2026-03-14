package com.akazukin.adapter.niconico;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.niconico.NiconicoClient;
import com.akazukin.sdk.niconico.NiconicoConfig;
import com.akazukin.sdk.niconico.model.NiconicoCommentResponse;
import com.akazukin.sdk.niconico.model.NiconicoUser;

import java.time.Instant;
import java.util.Objects;

public class NiconicoAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final String COMMENT_URL_PREFIX = "https://www.nicovideo.jp/watch/";
    private static final int NICONICO_MAX_LENGTH = 75;

    private final NiconicoClient client;

    public NiconicoAdapter(NiconicoClient client) {
        this.client = Objects.requireNonNull(client, "NiconicoClient must not be null");
    }

    public NiconicoAdapter(NiconicoConfig config) {
        this(NiconicoClient.builder().config(config).build());
    }

    public NiconicoAdapter() {
        this(new NiconicoConfig(
            System.getProperty("akazukin.niconico.api-key", System.getenv("NICONICO_API_KEY")),
            System.getProperty("akazukin.niconico.redirect-uri", System.getenv("NICONICO_REDIRECT_URI"))
        ));
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.NICONICO;
    }

    @Override
    public int getMaxContentLength() {
        return NICONICO_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return "https://oauth.nicovideo.jp/oauth2/authorize?state=" + state;
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            recordApiCall();
            return new SnsAuthToken(code, null, null, null);
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
        SnsProfile cached = getCachedProfile(accessToken);
        if (cached != null) {
            return cached;
        }
        try {
            checkRateLimit();
            NiconicoUser user = client.getProfile(accessToken);
            recordApiCall();
            SnsProfile profile = new SnsProfile(
                user.id(),
                user.nickname(),
                user.iconUrl(),
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
            // Niconico posts are comments on videos.
            // The content format is "videoId:comment" to specify target video.
            String content = request.content();
            String videoId;
            String comment;
            int colonIndex = content.indexOf(':');
            if (colonIndex > 0) {
                videoId = content.substring(0, colonIndex).trim();
                comment = content.substring(colonIndex + 1).trim();
            } else {
                videoId = "sm0";
                comment = content;
            }

            NiconicoCommentResponse response = client.postComment(accessToken, videoId, comment);
            recordApiCall();
            return new PostResult(
                response.commentId(),
                COMMENT_URL_PREFIX + videoId,
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
            recordApiCall();
            // Niconico does not support comment deletion via API
            throw new UnsupportedOperationException("Niconico does not support comment deletion");
        } catch (UnsupportedOperationException e) {
            throw wrapException("deletePost", e);
        }
    }

    @Override
    public void close() {
        // Resources are shared statics, no cleanup needed per instance
    }
}
