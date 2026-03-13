package com.akazukin.domain.port;

import com.akazukin.domain.model.PostRequest;
import com.akazukin.domain.model.PostResult;
import com.akazukin.domain.model.SnsAuthToken;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

public interface SnsAdapter {

    SnsPlatform platform();

    String getAuthorizationUrl(String callbackUrl, String state);

    SnsAuthToken exchangeToken(String code, String callbackUrl);

    SnsAuthToken refreshToken(String refreshToken);

    SnsProfile getProfile(String accessToken);

    PostResult post(String accessToken, PostRequest request);

    void deletePost(String accessToken, String postId);
}
