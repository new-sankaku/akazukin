package com.akazukin.domain.port;

import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsProfile;

import java.util.List;

public interface SnsGraphAdapter {

    SnsPlatform platform();

    List<SnsProfile> getFollowers(String accessToken, int limit);

    List<SnsProfile> getFollowing(String accessToken, int limit);
}
