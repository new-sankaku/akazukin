package com.akazukin.domain.port;

import com.akazukin.domain.model.SnsPlatform;

public interface SnsInteractionAdapter {

    SnsPlatform platform();

    void reply(String accessToken, String postId, String content);

    void mention(String accessToken, String userId, String content);
}
