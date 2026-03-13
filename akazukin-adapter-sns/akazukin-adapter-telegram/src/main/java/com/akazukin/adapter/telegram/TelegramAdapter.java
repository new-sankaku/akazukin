package com.akazukin.adapter.telegram;

import com.akazukin.adapter.core.AbstractSnsAdapter;
import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

public class TelegramAdapter extends AbstractSnsAdapter {

    @Override
    public SnsPlatform platform() {
        return SnsPlatform.TELEGRAM;
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, String state) {
        // TODO: Implement Telegram OAuth authorization URL generation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken exchangeToken(String code, String callbackUrl) {
        // TODO: Implement Telegram token exchange
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsAuthToken refreshToken(String refreshToken) {
        // TODO: Implement Telegram token refresh
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public SnsProfile getProfile(String accessToken) {
        // TODO: Implement Telegram profile retrieval
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PostResult post(String accessToken, PostRequest request) {
        // TODO: Implement Telegram post creation
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void deletePost(String accessToken, String postId) {
        // TODO: Implement Telegram post deletion
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
