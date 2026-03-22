package com.akazukin.application.dto;

import java.util.List;

public record PlatformSuccessRateDto(
    String platform,
    double overallRate,
    List<DailyRateDto> dailyRates
) {}
