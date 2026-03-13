package com.akazukin.adapter.bluesky;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

public class BlueskyAdapter extends AbstractSnsAdapter {

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.BLUESKY;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        // TODO: Implement Bluesky OAuth authorization URL generation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        // TODO: Implement Bluesky token exchange
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        // TODO: Implement Bluesky token refresh
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        // TODO: Implement Bluesky profile retrieval
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        // TODO: Implement Bluesky post creation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        // TODO: Implement Bluesky post deletion
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
