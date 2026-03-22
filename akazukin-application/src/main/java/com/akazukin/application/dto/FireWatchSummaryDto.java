package com.akazukin.application.dto;

import java.util.List;

public record FireWatchSummaryDto(
    long monitoredCount,
    long normalCount,
    long cautionCount,
    long criticalCount,
    List<FireWatchPostDto> alertPosts
) {}
