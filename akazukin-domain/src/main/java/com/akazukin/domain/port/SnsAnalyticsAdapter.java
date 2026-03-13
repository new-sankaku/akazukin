package com.akazukin.domain.port;

import com.akazukin.domain.model.AccountStats;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.model.SnsPlatform;

import java.util.Optional;

public interface SnsAnalyticsAdapter {

    SnsPlatform platform();

    Optional<SnsPostStats> getPostStats(String accessToken, String platformPostId);

    Optional<AccountStats> getAccountStats(String accessToken);
}
