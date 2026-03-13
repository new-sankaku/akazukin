package com.akazukin.adapter.pinterest;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

public class PinterestAdapter extends AbstractSnsAdapter {

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.PINTEREST;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        // TODO: Implement Pinterest OAuth authorization URL generation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        // TODO: Implement Pinterest token exchange
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        // TODO: Implement Pinterest token refresh
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        // TODO: Implement Pinterest profile retrieval
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        // TODO: Implement Pinterest post creation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        // TODO: Implement Pinterest post deletion
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
