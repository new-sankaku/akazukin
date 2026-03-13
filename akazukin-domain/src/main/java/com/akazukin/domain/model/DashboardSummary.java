package com.akazukin.domain.model;

import java.util.List;
import java.util.Map;

public record DashboardSummary(
    int totalPosts,
    int publishedPosts,
    int failedPosts,
    int scheduledPosts,
    int connectedAccounts,
    Map<SnsPlatform, Integer> postCountByPlatform,
    List<AccountStats> accountStats
) {}
