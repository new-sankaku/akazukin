package com.akazukin.application.dto;

import java.util.List;
import java.util.Map;

public record AnalyticsResponseDto(
    int totalPosts,
    int publishedPosts,
    int failedPosts,
    int scheduledPosts,
    int connectedAccounts,
    Map<String, Integer> postCountByPlatform,
    List<AccountStatsDto> accountStats
) {}
