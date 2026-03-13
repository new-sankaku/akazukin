package com.akazukin.adapter.instagram;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

public class InstagramAdapter extends AbstractSnsAdapter {

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.INSTAGRAM;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        // TODO: Implement Instagram OAuth authorization URL generation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        // TODO: Implement Instagram token exchange
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        // TODO: Implement Instagram token refresh
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        // TODO: Implement Instagram profile retrieval
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        // TODO: Implement Instagram post creation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        // TODO: Implement Instagram post deletion
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
