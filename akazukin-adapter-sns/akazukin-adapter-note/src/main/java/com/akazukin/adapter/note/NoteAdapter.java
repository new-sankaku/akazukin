package com.akazukin.adapter.note;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;
import com.akazukin.sdk.note.NoteClient;
import com.akazukin.sdk.note.NoteConfig;
import com.akazukin.sdk.note.model.NotePostResponse;
import com.akazukin.sdk.note.model.NoteUser;

import java.time.Instant;
import java.util.Objects;

public class NoteAdapter extends AbstractSnsAdapter implements AutoCloseable {

    private static final String POST_URL_PREFIX = "https://note.com/n/";
    private static final int NOTE_MAX_LENGTH = 50000;

    private final NoteClient client;

    public NoteAdapter(NoteClient client) {
        this.client = Objects.requireNonNull(client, "NoteClient must not be null");
    }

    public NoteAdapter(NoteConfig config) {
        this(NoteClient.builder().config(config).build());
    }

    public NoteAdapter() {
        this(new NoteConfig(
            System.getProperty("akazukin.note.api-key", System.getenv("NOTE_API_KEY")),
            System.getProperty("akazukin.note.redirect-uri", System.getenv("NOTE_REDIRECT_URI"))
        ));
    }

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.NOTE;
    }

    @Override
    public int getMaxContentLength() {
        return NOTE_MAX_LENGTH;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        return "https://note.com/oauth/authorize?state=" + state;
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        try {
            checkRateLimit();
            recordApiCall();
            // note.com uses API key authentication; token exchange returns the API key as-is
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
            // note.com API key based auth does not require token refresh
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
            NoteUser user = client.getProfile(accessToken);
            recordApiCall();
            SnsProfile profile = new SnsProfile(
                user.urlname(),
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
            // For note.com, the content serves as both title and body.
            // Extract the first line as title, rest as body.
            String content = request.content();
            String title;
            String body;
            int newlineIndex = content.indexOf('\n');
            if (newlineIndex > 0) {
                title = content.substring(0, newlineIndex).trim();
                body = content.substring(newlineIndex + 1).trim();
            } else {
                title = content;
                body = content;
            }

            NotePostResponse response = client.post(accessToken, title, body);
            recordApiCall();
            return new PostResult(
                response.id(),
                POST_URL_PREFIX + response.key(),
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
}
